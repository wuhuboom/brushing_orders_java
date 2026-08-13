package com.brushing.api.controller.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * User wallet request and response.
 */
public class UserWalletDto {

    /** Wallet record ID; returned to clients but not accepted as input. */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    /** Wallet name or currency. */
    private String wallet;

    /** Wallet address. */
    private String address;

    /** Blockchain network. */
    private String network;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getWallet() {
        return wallet;
    }

    public void setWallet(String wallet) {
        this.wallet = wallet;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }
}
