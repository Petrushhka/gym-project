package com.gymproject.common.event.domain;

import com.gymproject.common.security.SexType;

public record ProfileInfo(String firstName,
                          String lastName,
                          String phoneNumber,
                          SexType sex) {
}
