package antifraud.service;

import antifraud.model.GlobalLimits;
import antifraud.repository.GlobalLimitsRepository;
import org.springframework.stereotype.Service;

@Service
public class GlobalLimitsService {

    private final GlobalLimitsRepository limitsRepo;
    private GlobalLimits globalLimits;

    public GlobalLimitsService(GlobalLimitsRepository limitsRepo) {
        this.limitsRepo = limitsRepo;
        this.globalLimits = limitsRepo.findById(1L)
                .orElseGet(() -> {
                    GlobalLimits gl = new GlobalLimits();
                    gl.setAllowedLimit(200L);
                    gl.setManualLimit(1500L);
                    return limitsRepo.save(gl);
                });
    }

    public long getAllowedLimit() {
        return globalLimits.getAllowedLimit();
    }

    public long getManualLimit() {
        return globalLimits.getManualLimit();
    }

    public void applyFeedback(String originalResult, String newFeedback, long amount) {

        if (originalResult == null || newFeedback == null) {
            throw new IllegalStateException("Unprocessable feedback: result or feedback is null");
        }

        if (isException(originalResult, newFeedback)) {
            throw new IllegalStateException("Unprocessable feedback");
        }

        long allowed = globalLimits.getAllowedLimit();
        long manual = globalLimits.getManualLimit();

        switch (originalResult) {
            case "ALLOWED":
                switch (newFeedback) {
                    case "MANUAL_PROCESSING":
                        allowed = decrease(allowed, amount);
                        break;
                    case "PROHIBITED":
                        allowed = decrease(allowed, amount);
                        manual = decrease(manual, amount);
                        break;
                }
                break;

            case "MANUAL_PROCESSING":
                switch (newFeedback) {
                    case "ALLOWED":
                        allowed = increase(allowed, amount);
                        break;
                    case "PROHIBITED":
                        manual = decrease(manual, amount);
                        break;
                }
                break;

            case "PROHIBITED":
                switch (newFeedback) {
                    case "ALLOWED":
                        allowed = increase(allowed, amount);
                        manual = increase(manual, amount);
                        break;
                    case "MANUAL_PROCESSING":
                        manual = increase(manual, amount);
                        break;
                }
                break;
        }

        globalLimits.setAllowedLimit(allowed);
        globalLimits.setManualLimit(manual);
        limitsRepo.save(globalLimits);
    }

    private long increase(long currentLimit, long valueFromTransaction) {
        double newLimit = 0.8 * currentLimit + 0.2 * valueFromTransaction;
        return (long) Math.ceil(newLimit);
    }
    private long decrease(long currentLimit, long valueFromTransaction) {
        double newLimit = 0.8 * currentLimit - 0.2 * valueFromTransaction;
        return (long) Math.ceil(newLimit);
    }

    private boolean isException(String originalResult, String newFeedback) {
        if ("ALLOWED".equals(originalResult) && "ALLOWED".equals(newFeedback)) {
            return true;
        }
        if ("MANUAL_PROCESSING".equals(originalResult) && "MANUAL_PROCESSING".equals(newFeedback)) {
            return true;
        }
        if ("PROHIBITED".equals(originalResult) && "PROHIBITED".equals(newFeedback)) {
            return true;
        }
        return false;
    }
}
