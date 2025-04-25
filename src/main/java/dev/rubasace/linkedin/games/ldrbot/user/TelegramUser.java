package dev.rubasace.linkedin.games.ldrbot.user;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import dev.rubasace.linkedin.games.ldrbot.group.TelegramGroup;
import dev.rubasace.linkedin.games.ldrbot.session.GameSession;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Objects;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Entity
public class TelegramUser {

    @Id
    private Long id;
    private String userName;
    private String firstName;
    private String lastName;
    @JsonIgnoreProperties("members")
    @ManyToMany(mappedBy = "members")
    private Set<TelegramGroup> groups;

    @JsonIgnoreProperties("user")
    @OneToMany(mappedBy = "user")
    private Set<GameSession> sessions;

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TelegramUser that = (TelegramUser) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
