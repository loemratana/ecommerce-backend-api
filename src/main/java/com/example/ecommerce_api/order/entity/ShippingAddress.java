package com.example.ecommerce_api.order.entity;


import com.example.ecommerce_api.user.entity.Address;
import com.example.ecommerce_api.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Copy of the delivery address taken when the order is placed, so later edits to or
 * deletion of the user's saved Address never change where a past order was shipped.
 */
@Getter
@Embeddable
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShippingAddress {

    @Column(name = "ship_recipient_name", length = 200, nullable = false)
    private String recipientName;

    @Column(name = "ship_phone", length = 30)
    private String phone;

    @Column(name = "ship_street", length = 150, nullable = false)
    private String street;

    @Column(name = "ship_city", length = 100, nullable = false)
    private String city;

    @Column(name = "ship_state", length = 100, nullable = false)
    private String state;

    @Column(name = "ship_zip_code", length = 20, nullable = false)
    private String zipCode;

    @Column(name = "ship_country", length = 100, nullable = false)
    private String country;

    public static ShippingAddress from(User user, Address address) {
        return new ShippingAddress(
                user.getFirstName() + " " + user.getLastName(),
                user.getPhone(),
                address.getStreet(),
                address.getCity(),
                address.getState(),
                address.getZipCode(),
                address.getCountry()
        );
    }
}
