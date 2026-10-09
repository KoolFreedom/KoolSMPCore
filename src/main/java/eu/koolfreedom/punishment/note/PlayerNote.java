package eu.koolfreedom.punishment.note;

import java.time.LocalDateTime;

public record PlayerNote(String author, String message, LocalDateTime timestamp) {
}
