package com.baytalhikmah.sovereign.domain;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.TimeZoneStorageType;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.generator.EventType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;

@Entity
public class PurchaseOrder {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @NotBlank
    @Column(unique = true, length = 20)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(nullable = false)
    private Customer customer;

    @Column(nullable = false, length = 2)
    private OrderStatus status = OrderStatus.NEW;

    @ColumnDefault("current_timestamp")
    @Generated(event = EventType.INSERT)
    @Column(nullable = false, updatable = false)
    private Instant placedAt;

    @TimeZoneStorage(TimeZoneStorageType.COLUMN)
    private ZonedDateTime deliveryPromisedFor;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> lines = new ArrayList<>();

    protected PurchaseOrder() {
    }

    public PurchaseOrder(String orderNumber, Customer customer) {
        this.orderNumber = orderNumber;
        this.customer = customer;
    }

    public OrderLine addLine(Product product, int quantity) {
        OrderLine line = new OrderLine(this, product, quantity, product.getPrice());
        lines.add(line);
        return line;
    }

    public void removeLine(OrderLine line) {
        if (lines.remove(line)) {
            line.detachFromOrder();
        }
    }

    public void moveTo(OrderStatus next) {
        if (!status.canMoveTo(next)) {
            throw new IllegalStateException("Order " + orderNumber + " cannot move from " + status + " to " + next);
        }
        status = next;
    }

    public Money total() {
        return lines.stream()
            .map(OrderLine::lineTotal)
            .reduce(Money::plus)
            .orElseThrow(() -> new IllegalStateException("Order " + orderNumber + " has no lines"));
    }

    public UUID getId() {
        return id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public Customer getCustomer() {
        return customer;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }

    public ZonedDateTime getDeliveryPromisedFor() {
        return deliveryPromisedFor;
    }

    public void promiseDelivery(ZonedDateTime localAppointment) {
        this.deliveryPromisedFor = localAppointment;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public List<OrderLine> getLines() {
        return Collections.unmodifiableList(lines);
    }

}
