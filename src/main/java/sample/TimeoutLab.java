package sample;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** One scenario: provider accepted the request, but the client saw a timeout. */
public final class TimeoutLab {
    public enum State { ACCEPTED, UNKNOWN_RECONCILE_REQUIRED }
    public record Request(String notificationKey, String syntheticPayload) {
        public Request {
            Objects.requireNonNull(notificationKey);
            Objects.requireNonNull(syntheticPayload);
        }
    }
    public record Outcome(State state, int clientAttempts) {}
    public enum LookupResult { ACCEPTED, NOT_FOUND }
    public interface AcceptanceLookup { LookupResult lookup(String correlationKey); }
    public static final class AmbiguousTimeout extends RuntimeException {}

    public interface Provider {
        boolean supportsStableKey();
        void send(String attemptKey, String payload);
    }

    /** Synthetic contract: optional dedupe by key + identical payload; no expiry. */
    public static final class FakeProvider implements Provider, AcceptanceLookup {
        private final boolean stableKey, alwaysTimeout;
        private final Map<String,String> accepted = new HashMap<>();
        private int calls, effects;
        public FakeProvider(boolean stableKey, boolean alwaysTimeout) {
            this.stableKey=stableKey;
            this.alwaysTimeout=alwaysTimeout;
        }
        public boolean supportsStableKey() { return stableKey; }
        public void send(String key, String payload) {
            calls++;
            if(stableKey && accepted.containsKey(key)) {
                if(!accepted.get(key).equals(payload)) throw new IllegalArgumentException("same key, different payload");
                if(alwaysTimeout) throw new AmbiguousTimeout();
                return; // The repeated request is acknowledged without another effect.
            }
            accepted.put(key,payload);
            effects++; // The provider accepts BEFORE the response is lost.
            throw new AmbiguousTimeout();
        }
        public LookupResult lookup(String key) { return accepted.containsKey(key)?LookupResult.ACCEPTED:LookupResult.NOT_FOUND; }
        public int calls() { return calls; }
        public int effects() { return effects; }
    }

    /** Intentionally broken: a new attempt key defeats even a deduping provider. */
    public static Outcome naiveRetry(Provider provider, Request request) {
        for(int attempt=1;attempt<=2;attempt++) {
            try {
                provider.send(request.notificationKey()+"-attempt-"+attempt,request.syntheticPayload());
                return new Outcome(State.ACCEPTED,attempt);
            } catch(AmbiguousTimeout ignored) { /* unsafe automatic retry */ }
        }
        return new Outcome(State.UNKNOWN_RECONCILE_REQUIRED,2);
    }

    /** Retry once only if provider promises dedupe, always using the same key/payload. */
    public static Outcome reference(Provider provider, Request request) {
        int budget=provider.supportsStableKey()?2:1;
        for(int attempt=1;attempt<=budget;attempt++) {
            try {
                provider.send(request.notificationKey(),request.syntheticPayload());
                return new Outcome(State.ACCEPTED,attempt);
            } catch(AmbiguousTimeout ignored) {
                // A timeout is uncertainty, not proof of failure or successful delivery.
            }
        }
        return new Outcome(State.UNKNOWN_RECONCILE_REQUIRED,budget);
    }

    /** A trusted positive lookup resolves uncertainty; absence is not proof of failure. */
    public static Outcome reconcile(Outcome current, String correlationKey, AcceptanceLookup lookup) {
        if(current.state()==State.ACCEPTED) return current;
        return lookup.lookup(correlationKey)==LookupResult.ACCEPTED
            ?new Outcome(State.ACCEPTED,current.clientAttempts()):current;
    }

    public static void main(String[] args) {
        Request request=new Request("demo-notification-001","synthetic greeting");
        FakeProvider naive=new FakeProvider(true,false);
        print("NAIVE",naiveRetry(naive,request),naive);
        FakeProvider dedupe=new FakeProvider(true,false);
        print("STABLE_KEY",reference(dedupe,request),dedupe);
        FakeProvider unsupported=new FakeProvider(false,false);
        Outcome uncertain=reference(unsupported,request);
        print("NO_DEDUPE",uncertain,unsupported);
        Outcome reconciled=reconcile(uncertain,request.notificationKey(),unsupported);
        System.out.printf("RECONCILE lookup=%s attempts=%d provider_effects=%d client_state=%s%n",unsupported.lookup(request.notificationKey()),reconciled.clientAttempts(),unsupported.effects(),reconciled.state());
    }
    private static void print(String label,Outcome outcome,FakeProvider provider) {
        System.out.printf("%s attempts=%d provider_effects=%d client_state=%s%n",label,outcome.clientAttempts(),provider.effects(),outcome.state());
    }
}
