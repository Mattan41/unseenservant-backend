package org.kruskopf.backend.component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.kruskopf.backend.auth.dto.AuthDTO;
import org.kruskopf.backend.config.Email;
import org.kruskopf.backend.user.CustomUserDetails;
import org.kruskopf.backend.user.entity.ProviderType;
import org.kruskopf.backend.user.entity.User;
import org.kruskopf.backend.user.entity.UserRole;
import org.kruskopf.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Component
public class CustomOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private RestClient restClient;
    @Value("${FRONTEND_URL}")
    private String frontendUrl;
    @Value("#{'${ADMIN_WHITELIST:}'.split(',')}")
    private List<String> adminWhitelist;

    @Value("#{'${USER_WHITELIST:}'.split(',')}")
    private List<String> userWhitelist;


    public CustomOAuth2SuccessHandler(UserRepository userRepository, OAuth2AuthorizedClientService authorizedClientService) {
        this.userRepository = userRepository;
        this.authorizedClientService = authorizedClientService;
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


        // Fetch OAuth2-provider that the user used to login
        ProviderType providerType = getProviderType((OAuth2AuthenticationToken) authentication);

        UserAttributes userAttributes;

        switch (providerType) {
            case GOOGLE -> userAttributes = getGoogleAttributes(oAuth2User);
            case GITHUB -> userAttributes = getGitHubAttributes(oAuth2User, accessToken);
            default -> throw new IllegalArgumentException("Unknown provider: " + providerType);
        }

        String email = userAttributes.email();
        String name = userAttributes.name();
        String providerId = userAttributes.providerId();


        // this is a temporary blocker to allow only whitelisted users to access the app
        if (userWhitelist != null && !userWhitelist.isEmpty() && adminWhitelist != null && !adminWhitelist.isEmpty()) {
            if (!userWhitelist.contains(email) && !adminWhitelist.contains(email)) {
                response.sendRedirect(frontendUrl + "/under-construction");
                return;
            }
        }


//       boolean isAdmin = adminWhitelist != null && adminWhitelist.contains(Objects.requireNonNull(email));
        boolean isAdmin = adminWhitelist != null && adminWhitelist.contains(email);


        UserRole role = isAdmin ? UserRole.ADMIN : UserRole.USER;

        // Find or Create the user in the database
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

        // Update Spring Security Context with new Authentication (using the user from DB)
        updateSpringSecurityContextWithNewAuthentication(authentication, user);

        // Set user data (AuthDTO) into session for easy frontend communication
        setUserDataIntoSessionForFrontendCommunication(request, user);

        // Redirect to frontend
        response.sendRedirect(frontendUrl + "/oauth-redirect");
    }

    private static void setUserDataIntoSessionForFrontendCommunication(HttpServletRequest request, User user) {
        AuthDTO authDTO = new AuthDTO(
                user.getId(),
                user.getUserName(),
                user.getEmail(),
                user.getRole().toString()
        );
        request.getSession().setAttribute("user", authDTO);
    }

    private static void updateSpringSecurityContextWithNewAuthentication(Authentication authentication, User user) {
        CustomUserDetails userDetails = new CustomUserDetails(user);
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(authentication.getDetails());

        SecurityContextHolder.getContext().setAuthentication(authToken);
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
