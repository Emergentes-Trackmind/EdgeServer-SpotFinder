package com.constructinsight.edgeserver.iot.infrastructure.web.mapper;

import com.constructinsight.edgeserver.iot.domain.model.IotDevice;
import com.constructinsight.edgeserver.iot.infrastructure.web.dto.IotDeviceRequestDto;
import com.constructinsight.edgeserver.iot.infrastructure.web.dto.IotDeviceResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.util.List;

/**
 * MapStruct Mapper for IoT Device DTOs
 * Automatically converts between domain entities and DTOs
 */
@Mapper(componentModel = "spring")
public interface IotDeviceMapper {

    IotDeviceResponseDto toResponseDto(IotDevice device);

    List<IotDeviceResponseDto> toResponseDtoList(List<IotDevice> devices);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "parkingId", ignore = true)
    @Mapping(target = "parkingSpotId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastCheckIn", expression = "java(getLastCheckInOrNow(dto.getLastCheckIn()))")
    IotDevice toEntity(IotDeviceRequestDto dto);

    default Instant getLastCheckInOrNow(Instant lastCheckIn) {
        return lastCheckIn != null ? lastCheckIn : Instant.now();
    }
}

