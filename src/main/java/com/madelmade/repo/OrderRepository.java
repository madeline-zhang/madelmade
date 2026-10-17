package com.madelmade.repo;

import com.madelmade.domain.Customer;
import com.madelmade.domain.CustomerOrder;
import com.madelmade.domain.OrderStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {
    Optional<CustomerOrder> findByStatusToken(String statusToken);

    List<CustomerOrder> findByStatusInOrderByCreatedAtDesc(Collection<OrderStatus> statuses);

    List<CustomerOrder> findAllByOrderByCreatedAtDesc();

    long countByStatusIn(Collection<OrderStatus> statuses);

    List<CustomerOrder> findByCustomerOrderByCreatedAtDesc(Customer customer);

    @Query("select o from CustomerOrder o where lower(o.contactEmail) = lower(?1) or o.customer.email = lower(?1) order by o.createdAt desc")
    List<CustomerOrder> findForEmail(String email);

    @Query("select count(o) from CustomerOrder o where upper(o.promoCode) = upper(?1)"
            + " and (lower(o.contactEmail) = lower(?2) or o.customer.email = lower(?2)) and o.status not in ?3")
    long countPromoUses(String code, String email, Collection<OrderStatus> excluding);

    List<CustomerOrder> findByRequestedDateBetweenAndStatusInOrderByRequestedDate(
            LocalDate from, LocalDate to, Collection<OrderStatus> statuses);

    List<CustomerOrder> findByRequestedDateAndStatusInAndReminderSentFalse(
            LocalDate date, Collection<OrderStatus> statuses);

    long countByCustomer(Customer customer);
}
