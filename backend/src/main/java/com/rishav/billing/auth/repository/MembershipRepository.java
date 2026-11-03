package com.rishav.billing.auth.repository;

import com.rishav.billing.auth.entity.AppUser;
import com.rishav.billing.auth.entity.Business;
import com.rishav.billing.auth.entity.Membership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MembershipRepository extends JpaRepository<Membership, UUID> {

    Optional<Membership> findByUserAndBusiness(AppUser user, Business business);

    List<Membership> findByUser(AppUser user);

    List<Membership> findByBusiness(Business business);
}
