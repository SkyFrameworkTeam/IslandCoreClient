package com.skyframework.islandcoreclient.state;

import java.util.UUID;

public record ClientAllyLocationView(UUID uuid, String name, double x, double y, double z) {
}
