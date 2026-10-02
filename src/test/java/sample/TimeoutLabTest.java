package sample;
import static sample.TimeoutLab.*;

/** Explicit assertions throw regardless of the JVM -ea switch. */
public final class TimeoutLabTest {
    private static int passed;
    private static Request request() { return new Request("test-notification-001","synthetic greeting"); }
    private static void equal(Object expected,Object actual) {
        if(!expected.equals(actual)) throw new AssertionError("expected "+expected+", got "+actual);
    }
    private static void run(String name,Runnable test) {
        test.run();passed++;System.out.println("PASS "+name);
    }
    public static void main(String[] args) {
        run("broken retry produces duplicate effect",()->{
            FakeProvider p=new FakeProvider(true,false);
            Outcome outcome=naiveRetry(p,request());
            equal(2,p.calls());equal(2,p.effects());equal(State.UNKNOWN_RECONCILE_REQUIRED,outcome.state());
        });
        run("stable key gets one effect and an acknowledgement",()->{
            FakeProvider p=new FakeProvider(true,false);
            Outcome outcome=reference(p,request());
            equal(2,p.calls());equal(1,p.effects());equal(State.ACCEPTED,outcome.state());
        });
        run("unsupported dedupe stops without blind resend",()->{
            FakeProvider p=new FakeProvider(false,false);
            Outcome outcome=reference(p,request());
            equal(1,p.calls());equal(1,p.effects());equal(State.UNKNOWN_RECONCILE_REQUIRED,outcome.state());
        });
        run("permanent response loss stays unknown after bounded retry",()->{
            FakeProvider p=new FakeProvider(true,true);
            Outcome outcome=reference(p,request());
            equal(2,p.calls());equal(1,p.effects());equal(State.UNKNOWN_RECONCILE_REQUIRED,outcome.state());
        });
        run("fake contract rejects reused key with changed payload",()->{
            FakeProvider p=new FakeProvider(true,false);
            try { p.send("key-001","original synthetic payload"); } catch(AmbiguousTimeout expected) {}
            boolean rejected=false;
            try { p.send("key-001","different synthetic payload"); } catch(IllegalArgumentException expected) { rejected=true; }
            equal(true,rejected);equal(1,p.effects());
        });
        run("positive acceptance lookup resolves unknown without sending",()->{
            FakeProvider p=new FakeProvider(false,false);
            Outcome unknown=reference(p,request());
            equal(State.UNKNOWN_RECONCILE_REQUIRED,unknown.state());
            equal(LookupResult.ACCEPTED,p.lookup(request().notificationKey()));
            Outcome resolved=reconcile(unknown,request().notificationKey(),p);
            equal(State.ACCEPTED,resolved.state());equal(1,p.calls());equal(1,p.effects());
        });
        run("missing lookup remains unknown without sending",()->{
            FakeProvider p=new FakeProvider(false,false);
            Outcome unknown=new Outcome(State.UNKNOWN_RECONCILE_REQUIRED,1);
            equal(LookupResult.NOT_FOUND,p.lookup("missing-key"));
            equal(State.UNKNOWN_RECONCILE_REQUIRED,reconcile(unknown,"missing-key",p).state());
            equal(0,p.calls());equal(0,p.effects());
        });
        System.out.println("RESULT tests="+passed+" failures=0");
    }
}
