package org.jaybill.jbio.core;

public abstract class AbstractNioChannel {
    abstract void ioEvent();

    abstract void userEvent(UserEvent event);
}
