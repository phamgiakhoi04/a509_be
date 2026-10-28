package com.A509.Security;
import com.A509.Entity.User;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
public class VersionedUserDetails extends org.springframework.security.core.userdetails.User {
    private final long tokenVersion;
    public VersionedUserDetails(User user) {
        super(user.getUsername(), user.getPassword(), user.isStatus(), true, true, true,
            List.of(new SimpleGrantedAuthority(user.getRole().getName())));
        tokenVersion = user.getTokenVersion();
    }
    public long getTokenVersion() { return tokenVersion; }
}
