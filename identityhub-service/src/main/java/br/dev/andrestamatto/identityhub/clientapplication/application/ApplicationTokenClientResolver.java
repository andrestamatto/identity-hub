package br.dev.andrestamatto.identityhub.clientapplication.application;

import java.util.List;
import java.util.UUID;

public interface ApplicationTokenClientResolver {

    List<ApplicationTokenClient> resolve(UUID applicationId);
}
