package dev.rubasace.linkedin.games_tracker.session;

import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

interface GameSessionRepository extends CrudRepository<GameSession, UUID> {
}
