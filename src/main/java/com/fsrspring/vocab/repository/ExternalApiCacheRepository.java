package com.fsrspring.vocab.repository;

import com.fsrspring.vocab.model.ExternalApiCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExternalApiCacheRepository extends JpaRepository<ExternalApiCache, Long> {
    Optional<ExternalApiCache> findByProviderAndCacheHash(ExternalApiCache.Provider provider, String cacheHash);
}
