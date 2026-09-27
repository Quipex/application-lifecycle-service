package com.appmanagement.domain.application.ports;

import com.appmanagement.domain.application.values.PublicationId;

public interface PublicationIdGenerator {

    PublicationId next();
}