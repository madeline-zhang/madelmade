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

    List<CustomerOrder> findByRequestedDateBetweenAndStatusInOrderByRequestedDate(
            LocalDate from, LocalDate to, Collection<OrderStatus> statuses);

    List<CustomerOrder> findByRequestedDateAndStatusInAndReminderSentFalse(
            LocalDate date, Collection<OrderStatus> statuses);

    long countByCustomer(Customer customer);
}
