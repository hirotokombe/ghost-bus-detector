package com.hirokombe.ghostbus.gtfs.jpa;

import java.io.Serializable;

public record StopTimeKey(String tripId, int stopSequence) implements Serializable {}
