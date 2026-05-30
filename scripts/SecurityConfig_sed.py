import re
with open('src/main/java/com/fsrspring/vocab/security/SecurityConfig.java', 'r') as f:
    text = f.read()

# Replace the authorizeHttpRequests block
new_auth_block = """            .exceptionHandling(e -> e.authenticationEntryPoint(
                new org.springframework.security.web.authentication.HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED)
            ))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )"""

text = re.sub(r'\.authorizeHttpRequests.*?\.anyRequest\(\)\.authenticated\(\)\s*\)', new_auth_block, text, flags=re.DOTALL)

with open('src/main/java/com/fsrspring/vocab/security/SecurityConfig.java', 'w') as f:
    f.write(text)
