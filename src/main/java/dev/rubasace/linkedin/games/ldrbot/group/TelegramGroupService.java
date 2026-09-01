package dev.rubasace.linkedin.games.ldrbot.group;

import dev.rubasace.linkedin.games.ldrbot.session.GameInfo;
import dev.rubasace.linkedin.games.ldrbot.session.GameType;
import dev.rubasace.linkedin.games.ldrbot.session.GameTypeAdapter;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUser;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUserAdapter;
import dev.rubasace.linkedin.games.ldrbot.user.TelegramUserService;
import dev.rubasace.linkedin.games.ldrbot.user.UserInfo;
import dev.rubasace.linkedin.games.ldrbot.util.LinkedinTimeUtils;
import org.hibernate.Hibernate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Transactional(readOnly = true)
@Service
public class TelegramGroupService {

    private final TelegramGroupRepository telegramGroupRepository;
    private final TelegramUserService telegramUserService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final GameTypeAdapter gameTypeAdapter;
    private final TelegramUserAdapter telegramUserAdapter;

    TelegramGroupService(final TelegramGroupRepository telegramGroupRepository, final TelegramUserService telegramUserService, final ApplicationEventPublisher applicationEventPublisher, final GameTypeAdapter gameTypeAdapter, final TelegramUserAdapter telegramUserAdapter) {
        this.telegramGroupRepository = telegramGroupRepository;
        this.telegramUserService = telegramUserService;
        this.applicationEventPublisher = applicationEventPublisher;
        this.gameTypeAdapter = gameTypeAdapter;
        this.telegramUserAdapter = telegramUserAdapter;
    }

    public Optional<TelegramGroup> findGroup(final Long chatId) {
        return telegramGroupRepository.findById(chatId);
    }

    public Optional<TelegramGroup> findGroup(final String uuid) {
        return telegramGroupRepository.findByUuid(uuid);
    }

    public TelegramGroup findGroupOrThrow(final ChatInfo chatInfo) throws GroupNotFoundException {
        return this.findGroup(chatInfo.chatId()).orElseThrow(() -> new GroupNotFoundException(chatInfo));
    }

    private TelegramGroup findGroupOrThrow(final Long chatId) throws GroupNotFoundException {
        return findGroupOrThrow(new ChatInfo(chatId, null, true));
    }

    public Set<TelegramUser> findMembers(final ChatInfo chatInfo) throws GroupNotFoundException {
        Set<TelegramUser> members = findGroupOrThrow(chatInfo)
                .getMembers();
        Hibernate.initialize(members);
        return members;
    }



    @Transactional
    public TelegramGroup registerOrUpdateGroup(final ChatInfo chatInfo) {
        return telegramGroupRepository.findById(chatInfo.chatId())
                                      .map(telegramGroup -> udpateGroupData(telegramGroup, chatInfo))
                                      .orElseGet(() -> this.createGroup(chatInfo));
    }

    @Transactional
    public void addUserToGroup(final ChatInfo chatInfo, final UserInfo userInfo) throws GroupNotFoundException {
        TelegramGroup telegramGroup = findGroupOrThrow(chatInfo);

        TelegramUser telegramUser = telegramUserService.findOrCreate(userInfo);
        if (telegramGroup.getMembers().contains(telegramUser)) {
            return;
        }
        telegramGroup.getMembers().add(telegramUser);
        telegramGroupRepository.save(telegramGroup);
        applicationEventPublisher.publishEvent(new UserJoinedGroupEvent(this, userInfo, chatInfo));
    }

    @Transactional
    public void removeUserFromGroup(final ChatInfo chatInfo, final UserInfo userInfo) throws GroupNotFoundException {
        TelegramGroup telegramGroup = findGroupOrThrow(chatInfo);

        Optional<TelegramUser> telegramUser = telegramUserService.find(userInfo);
        if (telegramUser.isEmpty() || !telegramGroup.getMembers().contains(telegramUser.get())) {
            return;
        }
        telegramGroup.getMembers().remove(telegramUser.get());
        // Close, never purge. Deleting the history would put the player back into the participation set of every past
        // day they were parked on, so a later recalculation of one of those days would run at a different denominator
        // as soon as they rejoined. After the close there is no open period, so they are taking part on the day they
        // come back and on every day after it.
        closeOpenPeriods(telegramGroup, telegramUser.get().getId(), LinkedinTimeUtils.todayGameDay());
        telegramGroupRepository.save(telegramGroup);
        applicationEventPublisher.publishEvent(new UserLeftGroupEvent(this, chatInfo, userInfo));
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public Stream<TelegramGroup> findGroupsWithMissingScores(final LocalDate gameDay) {
        return telegramGroupRepository.findGroupsWithMissingScores(gameDay);
    }

    /**
     * The ids of the group's members who were parked on {@code gameDay}. Returns a computed set, never a live
     * collection and never anything holding an entity, so the caller needs no transaction of its own and no period ever
     * escapes this one.
     */
    public Set<Long> listPlayersParkedOn(final Long chatId, final LocalDate gameDay) throws GroupNotFoundException {
        return findGroup(chatId)
                .map(telegramGroup -> telegramGroup.getMembers().stream()
                                                   .map(TelegramUser::getId)
                                                   .filter(userId -> telegramGroup.isParkedOn(userId, gameDay))
                                                   .collect(Collectors.toSet()))
                .orElseThrow(() -> new GroupNotFoundException(new ChatInfo(chatId, null, true)));
    }

    /**
     * Parks the player if they are taking part on today's game day, and brings them back if they are not. It carries no
     * desired state on purpose: two admins acting on a stale list converge on the state the later tap asked for.
     * <p>
     * Parking opens a period at today's game day; bringing a player back closes every open period of theirs at today's
     * game day. No period is ever removed — the history is what keeps past days computed with the participation that
     * was in force on them.
     */
    @Transactional
    public void togglePlayerParticipation(final Long chatId, final Long userId) throws GroupNotFoundException {
        TelegramGroup telegramGroup = findGroupOrThrow(chatId);
        Optional<TelegramUser> member = telegramGroup.getMembers().stream()
                                                     .filter(telegramUser -> telegramUser.getId().equals(userId))
                                                     .findFirst();
        // The player left the group between the list being drawn and the button being tapped: no orphan period, and no
        // announcement of a change that did not happen.
        if (member.isEmpty()) {
            return;
        }
        LocalDate todayGameDay = LinkedinTimeUtils.todayGameDay();
        boolean parked = !telegramGroup.isParkedOn(userId, todayGameDay);
        if (parked) {
            telegramGroup.getParkedPeriods().add(new ParkedPeriod(telegramGroup, userId, todayGameDay));
        } else {
            closeOpenPeriods(telegramGroup, userId, todayGameDay);
        }
        telegramGroupRepository.save(telegramGroup);
        applicationEventPublisher.publishEvent(new PlayerParticipationChangedEvent(this, chatId, telegramUserAdapter.adapt(member.get()), parked,
                                                                                   telegramGroup.getParticipatingMembers(todayGameDay).size()));
    }

    /**
     * Brings a player back because they submitted a result for {@code gameDay}: closes every open period of theirs that
     * started on or before that day. A result for a day strictly before the parking changes nothing and announces
     * nothing, and neither does a result that lands inside an already-closed period — re-opening, splitting or
     * shortening one would write a boundary for a day other than today and retroactively change that day's
     * participation set.
     */
    @Transactional
    public void unparkPlayerForResult(final Long chatId, final Long userId, final LocalDate gameDay) throws GroupNotFoundException {
        TelegramGroup telegramGroup = findGroupOrThrow(chatId);
        if (!closeOpenPeriods(telegramGroup, userId, gameDay)) {
            return;
        }
        telegramGroupRepository.save(telegramGroup);
        LocalDate todayGameDay = LinkedinTimeUtils.todayGameDay();
        telegramUserService.find(userId)
                           .ifPresent(telegramUser -> applicationEventPublisher.publishEvent(
                                   new PlayerParticipationChangedEvent(this, chatId, telegramUserAdapter.adapt(telegramUser), false,
                                                                        telegramGroup.getParticipatingMembers(todayGameDay).size())));
    }

    /**
     * Closes, at today's game day, every open period of {@code userId} that started on or before
     * {@code startedOnOrBefore}. Returns whether anything was closed.
     * <p>
     * The end boundary is always {@code todayGameDay()} and never {@code startedOnOrBefore}: closing at the day of the
     * result would write a boundary into the past and silently change that day's participation set. Since every
     * {@code startGameDay} is itself written as {@code todayGameDay()} at the moment of the write, {@code start <= today}
     * always holds, so the close can never invert a period — and the two close-all callers pass {@code todayGameDay()},
     * which for the same reason closes every open period.
     */
    private boolean closeOpenPeriods(final TelegramGroup telegramGroup, final Long userId, final LocalDate startedOnOrBefore) {
        LocalDate todayGameDay = LinkedinTimeUtils.todayGameDay();
        List<ParkedPeriod> openPeriods = telegramGroup.getParkedPeriods().stream()
                                                       .filter(parkedPeriod -> parkedPeriod.getUserId().equals(userId))
                                                       .filter(ParkedPeriod::isOpen)
                                                       .filter(parkedPeriod -> !startedOnOrBefore.isBefore(parkedPeriod.getStartGameDay()))
                                                       .toList();
        openPeriods.forEach(parkedPeriod -> parkedPeriod.setEndGameDay(todayGameDay));
        return !openPeriods.isEmpty();
    }

    private TelegramGroup udpateGroupData(final TelegramGroup telegramGroup, final ChatInfo chatInfo) {
        boolean active = telegramGroup.isActive();
        if (active && telegramGroup.getUuid() != null && telegramGroup.getGroupName().equals(chatInfo.title())) {
            return telegramGroup;
        }
        telegramGroup.setGroupName(chatInfo.title());
        telegramGroup.setActive(true);
        if (telegramGroup.getUuid() == null) {
            telegramGroup.setUuid(UUID.randomUUID().toString().replace("-", ""));
        }
        TelegramGroup updatedGroup = telegramGroupRepository.save(telegramGroup);
        if (!active) {
            applicationEventPublisher.publishEvent(new GroupCreatedEvent(this, chatInfo));
        }
        return updatedGroup;
    }


    private TelegramGroup createGroup(final ChatInfo chatInfo) {
        TelegramGroup telegramGroup = new TelegramGroup(chatInfo.chatId(), chatInfo.title());
        TelegramGroup createdTelegramGroup = telegramGroupRepository.save(telegramGroup);
        applicationEventPublisher.publishEvent(new GroupCreatedEvent(this, chatInfo));
        return createdTelegramGroup;
    }

    public Set<GameType> listTrackedGames(final ChatInfo chatInfo) throws GroupNotFoundException {
        TelegramGroup telegramGroup = findGroupOrThrow(chatInfo);
        return telegramGroup.getTrackedGames();
    }

    public Set<GameType> listTrackedGames(final Long chatId) throws GroupNotFoundException {
        return listTrackedGames(new ChatInfo(chatId, null, true));
    }

    public Set<GameType> listTrackedGames(final String groupId) throws GroupNotFoundException {
        return findGroup(groupId)
                .map(TelegramGroup::getTrackedGames)
                .orElseThrow(() -> new GroupNotFoundException(groupId));

    }

    public boolean isReadFromMessages(final ChatInfo chatInfo) throws GroupNotFoundException {
        TelegramGroup telegramGroup = findGroupOrThrow(chatInfo);
        return telegramGroup.isReadFromMessages();
    }

    public boolean isReadFromMessages(final Long chatId) throws GroupNotFoundException {
        return isReadFromMessages(new ChatInfo(chatId, null, true));
    }

    @Transactional
    public void removeGroup(final ChatInfo chatInfo) throws GroupNotFoundException {
        TelegramGroup telegramGroup = findGroupOrThrow(chatInfo);
        telegramGroup.setActive(false);
        telegramGroupRepository.save(telegramGroup);
    }

    @Transactional
    public void toggleGameTracking(final Long chatId, final GameType gameType) throws GroupNotFoundException {
        TelegramGroup telegramGroup = findGroupOrThrow(chatId);
        if (telegramGroup.getTrackedGames().contains(gameType)) {
            telegramGroup.getTrackedGames().remove(gameType);
        } else {
            telegramGroup.getTrackedGames().add(gameType);
        }
        telegramGroupRepository.save(telegramGroup);
        Set<GameInfo> trackedGames = telegramGroup.getTrackedGames().stream()
                                                  .map(gameTypeAdapter::adapt)
                                                  .collect(Collectors.toSet());
        applicationEventPublisher.publishEvent(new TrackedGamesChangedEvent(this, chatId, trackedGames));
    }

    @Transactional
    public void setTimezone(final Long chatId, final String timeZone) throws GroupNotFoundException {
        TelegramGroup telegramGroup = findGroupOrThrow(chatId);
        ZoneId timezone = ZoneId.of(timeZone);
        telegramGroup.setTimezone(timezone);
        telegramGroupRepository.save(telegramGroup);
        applicationEventPublisher.publishEvent(new TimezoneChangedEvent(this, chatId, timezone));
    }

    @Transactional
    public void setReadFromMessages(final Long chatId, final boolean readFromMessages) throws GroupNotFoundException {
        TelegramGroup telegramGroup = findGroupOrThrow(chatId);
        telegramGroup.setReadFromMessages(readFromMessages);
        telegramGroupRepository.save(telegramGroup);
    }
}
