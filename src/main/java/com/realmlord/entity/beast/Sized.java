package com.realmlord.entity.beast;

/** A creature whose size follows its level: young and weak ones small, old and strong ones large. */
public interface Sized {
    float getSize();

    void setSize(float size);

    /** The drawn size of this kind of creature at size 1 (a Boar King is drawn half again as big as a boar). */
    default float baseScale() {
        return 1.0F;
    }
}
