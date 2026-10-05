package com.team4.core.services;

import com.team4.core.dtos.response.FavoriteResponse;
import java.util.List;
import java.util.UUID;

public interface FavoriteService {
    FavoriteResponse add(UUID tenantId, UUID roomId);

    void remove(UUID tenantId, UUID roomId);

    List<FavoriteResponse> getAll(UUID tenantId);
}
