package in.joyway.dto.waitlist;

import lombok.Builder;

@Builder
public record WaitlistJoinResponse(
        boolean alreadyJoined,
        String email
) {
}
