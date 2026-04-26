package org.kruskopf.backend.component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.kruskopf.backend.auth.JwtService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.kruskopf.backend.config.Email;
import org.kruskopf.backend.user.entity.ProviderType;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.entity.UserRole;
import org.kruskopf.backend.user.repository.UserRepository;
import org.kruskopf.backend.whitelist.EmailWhitelistService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Component
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final EmailWhitelistService emailWhitelistService;
    private final JwtService jwtService;

    private RestClient restClient;

    @Value("${FRONTEND_URL}")
    private String frontendUrl;
    private final String[] allowedOrigins;

    public CustomOAuth2SuccessHandler(UserRepository userRepository,
                                      OAuth2AuthorizedClientService authorizedClientService,
                                      EmailWhitelistService emailWhitelistService,
                                      JwtService jwtService,
                                      @Value("${ALLOWED_ORIGINS}") String[] allowedOrigins) {
        this.userRepository = userRepository;
        this.authorizedClientService = authorizedClientService;
        this.emailWhitelistService = emailWhitelistService;
        this.jwtService = jwtService;
        this.allowedOrigins = allowedOrigins;
    }

    public void setRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauthToken.getPrincipal();
        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName()
        );
        OAuth2AccessToken accessToken = authorizedClient.getAccessToken();

        ProviderType providerType = getProviderType(oauthToken);

        UserAttributes userAttributes = switch (providerType) {
            case GOOGLE -> getGoogleAttributes(oAuth2User);
            case GITHUB -> getGitHubAttributes(oAuth2User, accessToken);
        };

        String email = userAttributes.email();
        String name = userAttributes.name();
        String providerId = userAttributes.providerId();

        if (!emailWhitelistService.isEmailWhitelisted(email)) {
            response.sendRedirect(frontendUrl + "/under-construction");
            return;
        }

        UserRole role = emailWhitelistService.getEmailRole(email)
                .orElse(UserRole.USER);

        User user = userRepository.findByProviderId(providerId)
                .orElseGet(() -> userRepository.save(new User(
                        providerId,
                        providerType,
                        email,
                        name,
                        email,
                        role,
                        "password"
                )));

        // Generate JWT token
        String jwtToken = jwtService.generateToken(user);

        /* TODO: Refactor redirect URL logic and cleanup
         Create a private helper method 'findMatchingOrigin(String candidate)' to eliminate redundant loops.
         Consolidate headers (X-Forwarded-Host, Referer, Origin) into a single resolution flow.
         Remove 'frontendUrl' dependency once dynamic resolution is verified stable across all environments.
         Improve GitHub attribute mapping (prefer 'name' over 'login' for display purposes).
         */

        // 1. Check cookie set by /api/auth/oauth-init (most reliable for cross-domain flows)
        String dynamicFrontendUrl = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("oauth_redirect_origin".equals(cookie.getName())) {
                    String cookieOrigin = cookie.getValue();
                    for (String allowed : allowedOrigins) {
                        if (allowed.equals(cookieOrigin)) {
                            dynamicFrontendUrl = allowed;
                            break;
                        }
                    }
                    ResponseCookie clear = ResponseCookie.from("oauth_redirect_origin", "")
                            .path("/")
                            .httpOnly(true)
                            .maxAge(0)
                            .secure(true)
                            .sameSite("Lax")
                            .build();
                    response.addHeader(HttpHeaders.SET_COOKIE, clear.toString());
                    break;
                }
            }
        }

        // 2. Fallback: X-Forwarded-Host (Nginx sets this value)
        if (dynamicFrontendUrl == null) {
            String forwardedHost = request.getHeader("X-Forwarded-Host");
            if (forwardedHost != null && !forwardedHost.isEmpty()) {
                String protocol = request.getHeader("X-Forwarded-Proto");
                if (protocol == null || protocol.isEmpty()) {
                    protocol = "https";
                }
                String fullForwardedUrl = protocol + "://" + forwardedHost.split(":")[0];
                for (String allowed : allowedOrigins) {
                    if (fullForwardedUrl.equalsIgnoreCase(allowed)) {
                        dynamicFrontendUrl = allowed;
                        break;
                    }
                }
            }
        }

        // 3. Fallback: Referer / Origin header
        if (dynamicFrontendUrl == null) {
            String origin = request.getHeader("Referer");
            if (origin == null || origin.isEmpty()) {
                origin = request.getHeader("Origin");
            }
            if (origin != null) {
                for (String allowed : allowedOrigins) {
                    if (origin.startsWith(allowed)) {
                        dynamicFrontendUrl = allowed;
                        break;
                    }
                }
            }
        }

        if (dynamicFrontendUrl == null) {
            dynamicFrontendUrl = frontendUrl;
        }

        // Redirect to frontend with token as query parameter
        String redirectUrl = UriComponentsBuilder.fromUriString(dynamicFrontendUrl + "/oauth-redirect")
                .queryParam("token", URLEncoder.encode(jwtToken, StandardCharsets.UTF_8))
                .build()
                .toUriString();
        response.sendRedirect(redirectUrl);
    }

    private static ProviderType getProviderType(OAuth2AuthenticationToken authentication) {
        String registrationId = authentication.getAuthorizedClientRegistrationId();

        // Define ProviderType based on registrationId
        ProviderType providerType;
        if ("google".equalsIgnoreCase(registrationId)) {
            providerType = ProviderType.GOOGLE;
        } else if ("github".equalsIgnoreCase(registrationId)) {
            providerType = ProviderType.GITHUB;
        } else {
            throw new IllegalArgumentException("Unknown provider: " + registrationId);
        }
        return providerType;
    }

    private UserAttributes getGoogleAttributes(OAuth2User oAuth2User) {
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String providerId = oAuth2User.getAttribute("sub");
        return new UserAttributes(email, name, providerId);
    }

    private UserAttributes getGitHubAttributes(OAuth2User oAuth2User, OAuth2AccessToken accessToken) {
        String email = oAuth2User.getAttribute("email");
        if (email == null) {
            // Fetch email from GitHub API
            List<Email> emails = getEmails(accessToken);
            for (Email emailEntry : emails) {
                if (emailEntry.primary() && emailEntry.verified()) {
                    email = emailEntry.email();
                    break;
                }
            }
            if (email == null) {
                email = "unknown@github.com";
            }
        }
        String name = oAuth2User.getAttribute("login");
        Integer providerId = oAuth2User.getAttribute("id");
        if (providerId == null) {
            throw new IllegalArgumentException("Provider ID is null");
        }
        return new UserAttributes(email, name, providerId.toString());
    }

    @Retryable
    public List<Email> getEmails(OAuth2AccessToken accessToken) {
        System.out.println("Getting emails from GitHub...");
        return restClient.get()
                .uri("https://api.github.com/user/emails")
                .headers(headers -> headers.setBearerAuth(accessToken.getTokenValue()))
                .accept(APPLICATION_JSON)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    private record UserAttributes(String email, String name, String providerId) {
    }

}
