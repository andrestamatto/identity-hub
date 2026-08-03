package br.dev.andrestamatto.identityhub.access.adapter.in.http;

import br.dev.andrestamatto.identityhub.access.application.GrantMembership;
import br.dev.andrestamatto.identityhub.access.application.MembershipGrantResult;
import br.dev.andrestamatto.identityhub.clientapplication.application
        .MembershipProvisioningClientResolver;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/memberships")
final class MembershipGrantController {

    private final MembershipProvisioningClientResolver provisioningClientResolver;
    private final GrantMembership grantMembership;
    private final MembershipGrantMetrics metrics;

    MembershipGrantController(
            MembershipProvisioningClientResolver provisioningClientResolver,
            GrantMembership grantMembership,
            MembershipGrantMetrics metrics) {
        this.provisioningClientResolver = provisioningClientResolver;
        this.grantMembership = grantMembership;
        this.metrics = metrics;
    }

    @PostMapping
    ResponseEntity<MembershipGrantResponse> grant(
            JwtAuthenticationToken authentication,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody MembershipGrantRequest request) {
        var authorizedParty = authentication.getToken().getClaimAsString("azp");
        var provisioner = provisioningClientResolver.resolve(authorizedParty)
                .orElseThrow(MembershipProvisioningDeniedException::new);
        var result = metrics.record(() -> grantMembership.execute(new GrantMembership.Command(
                provisioner.applicationId(),
                provisioner.applicationClientId(),
                request.userAccountRef(),
                idempotencyKey,
                MDC.get("correlationId"))));
        return ResponseEntity.accepted().body(MembershipGrantResponse.from(result));
    }

    record MembershipGrantRequest(UUID userAccountRef) {

        MembershipGrantRequest {
            if (userAccountRef == null) {
                throw new IllegalArgumentException("User account reference is required");
            }
        }
    }

    record MembershipGrantResponse(
            UUID operationId,
            UUID membershipId,
            String state,
            Instant acceptedAt) {

        static MembershipGrantResponse from(MembershipGrantResult result) {
            return new MembershipGrantResponse(
                    result.operationId(),
                    result.membershipId(),
                    result.state(),
                    result.acceptedAt());
        }
    }
}
