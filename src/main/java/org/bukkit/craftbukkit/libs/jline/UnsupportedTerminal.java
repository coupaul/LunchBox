package org.bukkit.craftbukkit.libs.jline;

public class UnsupportedTerminal extends TerminalSupport {

    public UnsupportedTerminal() {
        super(false);
        this.setAnsiSupported(false);
        this.setEchoEnabled(true);
    }
}
