package com.caspian.client.impl.event.entity;

import com.caspian.client.api.event.Event;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class EntityPositionEvent extends Event
{
    private final Vec3d updatePos;
    private final Vec3d prevPos;

    public EntityPositionEvent(Vec3d updatePos, Vec3d prevPos)
    {
        this.updatePos = updatePos;
        this.prevPos = prevPos;
    }

    public Vec3d getUpdatePos()
    {
        return updatePos;
    }

    public Vec3d getPrevPos()
    {
        return prevPos;
    }
}
