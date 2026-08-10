package com.college.erp.module.hostel.dto;

import java.util.UUID;

import com.college.erp.module.hostel.domain.Room;
import com.college.erp.module.hostel.domain.RoomType;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoomDto {

    private UUID id;
    private UUID blockId;
    private String blockName;
    private String roomNumber;
    private Integer floor;
    private int capacity;
    private RoomType roomType;

    public static RoomDto from(Room room) {
        return RoomDto.builder()
                .id(room.getId())
                .blockId(room.getBlock().getId())
                .blockName(room.getBlock().getName())
                .roomNumber(room.getRoomNumber())
                .floor(room.getFloor())
                .capacity(room.getCapacity())
                .roomType(room.getRoomType())
                .build();
    }
}
