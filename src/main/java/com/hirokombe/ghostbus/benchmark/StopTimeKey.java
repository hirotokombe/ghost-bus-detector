package com.hirokombe.ghostbus.benchmark;

import java.io.Serializable;

public record StopTimeKey(String tripId, int stopSequence) implements Serializable {}
