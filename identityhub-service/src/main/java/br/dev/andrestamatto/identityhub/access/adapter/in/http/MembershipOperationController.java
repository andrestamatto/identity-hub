package br.dev.andrestamatto.identityhub.access.adapter.in.http;

import br.dev.andrestamatto.identityhub.access.application.GetMembershipOperation;
import br.dev.andrestamatto.identityhub.access.application.MembershipOperationStatus;
import br.dev.andrestamatto.identityhub.clientapplication.application
        .MembershipProvisioningClientResolver;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/membership-operations")
final class MembershipOperationController {

    private final MembershipProvisioningClientResolver provisioningClientResolver;
    private final GetMembershipOperation getMembershipOperation;

    MembershipOperationController(
            MembershipProvisioningClientResolver provisioningClientResolver,
            GetMembershipOperation getMembershipOperation) {
        this.provisioningClientResolver = provisioningClientResolver;
        this.getMembershipOperation = getMembershipOperation;
    }

    @GetMapping("/{operationId}")
    MembershipOperationResponse get(
            JwtAuthenticationToken authentication,
            @PathVariable UUID operationId) {
        var provisioner = provisioningClientResolver
                .resolve(authentication.getToken().getClaimAsString("azp"))
                .orElseThrow(MembershipProvisioningDeniedException::new);
        return getMembershipOperation.execute(operationId, provisioner.applicationId())
                .map(MembershipOperationResponse::from)
                .orElseThrow(MembershipOperationNotFoundException::new);
    }

    record MembershipOperationResponse(
            UUID operationId,
            UUID membershipId,
            String membershipState,
            String projectionState,
            int attempts,
            String lastFailureCode,
            Instant acceptedAt,
            Instant updatedAt) {

        static MembershipOperationResponse from(MembershipOperationStatus status) {
            return new MembershipOperationResponse(
                    status.operationId(),
                    status.membershipId(),
                    status.membershipState(),
                    status.projectionState(),
                    status.attempts(),
                    status.lastFailureCode(),
                    status.acceptedAt(),
                    status.updatedAt());
        }
    }
}
