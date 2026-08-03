package br.dev.andrestamatto.identityhub.access.adapter.in.http;

import br.dev.andrestamatto.identityhub.access.application.GetMembershipOperation;
import br.dev.andrestamatto.identityhub.access.application.MembershipOperationStatus;
import br.dev.andrestamatto.identityhub.access.application.ReconcileMembershipOperation;
import br.dev.andrestamatto.identityhub.clientapplication.application
        .MembershipProvisioningClientResolver;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/membership-operations")
final class MembershipOperationController {

    private final MembershipProvisioningClientResolver provisioningClientResolver;
    private final GetMembershipOperation getMembershipOperation;
    private final ReconcileMembershipOperation reconcileMembershipOperation;

    MembershipOperationController(
            MembershipProvisioningClientResolver provisioningClientResolver,
            GetMembershipOperation getMembershipOperation,
            ReconcileMembershipOperation reconcileMembershipOperation) {
        this.provisioningClientResolver = provisioningClientResolver;
        this.getMembershipOperation = getMembershipOperation;
        this.reconcileMembershipOperation = reconcileMembershipOperation;
    }

    @PostMapping("/{operationId}/projection/reconcile")
    ResponseEntity<MembershipOperationResponse> reconcile(
            JwtAuthenticationToken authentication,
            @PathVariable UUID operationId) {
        var provisioner = provisioningClientResolver
                .resolve(authentication.getToken().getClaimAsString("azp"))
                .orElseThrow(MembershipProvisioningDeniedException::new);
        var response = reconcileMembershipOperation
                .execute(operationId, provisioner.applicationId())
                .map(MembershipOperationResponse::from)
                .orElseThrow(MembershipOperationNotFoundException::new);
        return ResponseEntity.accepted().body(response);
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
