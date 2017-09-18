package com.greysonparrelli.permiso;

/**
 * FretXapp for FretX
 * Created by pandor on 21/04/17 01:43.
 */

public enum Result {
    /**
     * The permission was granted.
     */
    GRANTED,

    /**
     * The permission was denied, but not permanently.
     */
    DENIED,

    /**
     * The permission was permanently denied.
     */
    PERMANENTLY_DENIED
}
