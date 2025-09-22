package org.example.reportsystem.service;

import lombok.RequiredArgsConstructor;
import org.example.reportsystem.model.User;
import org.example.reportsystem.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {
    private final UserRepository users;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User u = users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("not found"));
        var auths = List.of(new SimpleGrantedAuthority(u.getRole().name()));
        return new org.springframework.security.core.userdetails.User(
                u.getUsername(), u.getPassword(), auths);
    }
}
