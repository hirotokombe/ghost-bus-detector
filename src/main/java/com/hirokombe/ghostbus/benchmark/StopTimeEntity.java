package com.hirokombe.ghostbus.benchmark;

import com.hirokombe.ghostbus.gtfs.model.GtfsStopTime;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "stop_times")
@IdClass(StopTimeKey.class)
public class StopTimeEntity implements Persistable<StopTimeKey> {

    @Id
    private String tripId;
    @Id
    private int stopSequence;
    private String stopId;
    private Integer arrivalTimeSeconds;
    private Integer departureTimeSeconds;
    private String stopHeadsign;
    private Integer pickupType;
    private Integer dropOffType;
    private Double shapeDistTraveled;
    private Integer timepoint;

    protected StopTimeEntity() {}

    private StopTimeEntity(String tripId, int stopSequence, String stopId,
                           Integer arrivalTimeSeconds, Integer departureTimeSeconds, String stopHeadsign,
                           Integer pickupType, Integer dropOffType, Double shapeDistTraveled, Integer timepoint) {
        this.tripId = tripId;
        this.stopSequence = stopSequence;
        this.stopId = stopId;
        this.arrivalTimeSeconds = arrivalTimeSeconds;
        this.departureTimeSeconds = departureTimeSeconds;
        this.stopHeadsign = stopHeadsign;
        this.pickupType = pickupType;
        this.dropOffType = dropOffType;
        this.shapeDistTraveled = shapeDistTraveled;
        this.timepoint = timepoint;
    }

    public static StopTimeEntity from(GtfsStopTime stopTime) {
        return new StopTimeEntity(
                stopTime.tripId(),
                stopTime.stopSequence(),
                stopTime.stopId(),
                stopTime.arrivalTimeSeconds(),
                stopTime.departureTimeSeconds(),
                stopTime.stopHeadsign(),
                stopTime.pickupType(),
                stopTime.dropOffType(),
                stopTime.shapeDistTraveled(),
                stopTime.timepoint()
        );
    }

    @Override
    public StopTimeKey getId() {
        return new StopTimeKey(tripId, stopSequence);
    }

    @Override
    public boolean isNew() {
        return true;
    }
}
