package chat;

import java.io.Serializable;

public class Messages implements Comparable<Messages>, Serializable {

    private static final long serialVersionUID = 1L;

    String mId, sender, sentMsg;
    int pId, clock;

    public Messages(String mId, int pId, String sender, String sentMsg, int clock) {
        this.mId = mId;
        this.pId = pId;
        this.sender = sender;
        this.sentMsg = sentMsg;
        this.clock = clock;
    }

    @Override
    public String toString() {
        return sender + ": " + sentMsg;
    }

    // Total Order using logical clocks
    @Override
    public int compareTo(Messages o) {
        // Tie Breaker
        if (this.clock == o.clock) {
            return this.pId - o.pId;
        }
        return this.clock - o.clock;
    }

}
