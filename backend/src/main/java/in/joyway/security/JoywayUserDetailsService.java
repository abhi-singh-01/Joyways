package in.joyway.security;

import in.joyway.domain.entity.User;
import in.joyway.domain.repository.UserRepository;
import in.joyway.util.EmailUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JoywayUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(EmailUtils.normalize(email))
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials."));
        return new JoywayUserDetails(user);
    }
}
