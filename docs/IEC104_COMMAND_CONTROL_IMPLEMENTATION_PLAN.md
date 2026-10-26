# Multi-Gateway Command Control Implementation Plan

## Objective

Add a controlled breaker/disconnector command flow with command-topic integration, a command monitor, gateway routing, device-state confirmation, and timeout alerts.

The command monitor is a separate protocol-independent backend service. Phase 1 covers repository boundaries, gateway capabilities, and the shared simulator contract. The monitor owns locking, persistence, timers, recovery and alerts; gateways own protocol translation, receipt reporting and mapped feedback. Frontend implementation remains out of scope. The first simulator target remains an IEC 60870-5-104 simulator. The initial timing configuration is:

- Initial response timeout: **2 seconds**
- Completion timeout: **8 seconds**

Both values must be configurable in the database. The values are copied into the active command when it starts, so changing configuration does not change an already-running command.

## Core behavior

Only one command may be active for a device. The command contract accepts the requested target state, and the backend validates it against the latest confirmed state immediately before sending because the caller's state may be stale.

The command monitor uses two timers:

1. **Initial response timer** starts after the gateway/device acknowledges receipt of the command. If neither an intermediate state nor the requested final state arrives within 2 seconds, the monitor emits a `COMMAND_NO_STATE_CHANGE` alert.
2. **Completion timer** starts when an intermediate state such as `TRANSITION` is received. The initial timer is cancelled. If the requested final state does not arrive within 8 seconds, the monitor emits a `COMMAND_COMPLETION_TIMEOUT` alert.

If the final state arrives at either stage, the command succeeds, its timer is cancelled, and the device lock is released.

## Message contracts

All messages use JSON and include `schemaVersion: 1`, a unique `messageId`, and ISO-8601 timestamps.

### Command request

Topic: `die/v1/control/commands`

```json
{
  "messageId": "msg-01J...",
  "commandId": "cmd-01J...",
  "deviceId": "breaker-123",
  "gatewayId": "gateway-456",
  "commandType": "SWITCH",
  "targetState": "OFF",
  "expectedIntermediateState": "TRANSITION",
  "requestedBy": "user-789",
  "requestedAt": "2026-10-05T10:15:00Z",
  "schemaVersion": 1
}
```

### Gateway command acknowledgement

Topic: `die/v1/control/acknowledgements`

`acknowledgement` is either `ACCEPTED` or `REJECTED`. The monitor starts the initial timer only for `ACCEPTED`.

### Device state

Topic: `die/v1/measurements/states`

The state is normally `ON`, `OFF`, or `TRANSITION`. IEC-104 double-point values (`11` and `00`) are mapped by the gateway to the canonical states. Invalid or indeterminate values must be preserved as `UNKNOWN` or `INVALID`, not treated as success.

### Command status

Topic: `die/v1/control/status`

Recommended statuses: `VALIDATING`, `BLOCKED`, `SENT`, `WAITING_FOR_INITIAL_CHANGE`, `WAITING_FOR_FINAL_STATE`, `SUCCEEDED`, `REJECTED`, `COMMUNICATION_FAILED`, and `TIMED_OUT`.

### Alerts

Topic: `die/v1/alerts`

Initial alert types:

- `COMMAND_NO_STATE_CHANGE`
- `COMMAND_COMPLETION_TIMEOUT`
- `COMMAND_REJECTED`
- `COMMAND_COMMUNICATION_FAILED`

Every alert includes `commandId`, `deviceId`, `targetState`, `lastObservedState`, the timeout used, and the timestamp at which the deadline expired.

## Database migration

Create a Flyway migration for shared timing configuration:

```sql
CREATE TABLE command_timing_config (
    id BIGSERIAL PRIMARY KEY,
    device_type VARCHAR(50) NOT NULL UNIQUE,
    initial_response_timeout_ms INTEGER NOT NULL,
    completion_timeout_ms INTEGER NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_initial_response_timeout_positive
        CHECK (initial_response_timeout_ms > 0),
    CONSTRAINT chk_completion_timeout_positive
        CHECK (completion_timeout_ms > 0)
);

INSERT INTO command_timing_config (
    device_type,
    initial_response_timeout_ms,
    completion_timeout_ms
) VALUES ('BREAKER_DISCONNECTOR', 2000, 8000);
```

The active command record must retain the copied timeout values and its current phase. Timers must be recoverable after a command-monitor restart so a device cannot remain permanently locked.

## Delivery phases

Each phase after Phase 1 has one small outcome and one concrete gate.

### Phase 1 - Audit all gateways and define the shared contract

**Purpose:** Audit IEC-104, Modbus, SterPMU, OpenPMU and RSocket control ingress before finalizing the shared command-monitor contract.

**Deliverables:**

- Document actual topics, payloads, identifiers, command capabilities, receipt semantics, feedback quality, polling latency, and correlation support.
- Classify each gateway as equipment control, feedback only, stream management, or unsupported for `SWITCH`.
- Separate physical device identity from gateway, RTU, PMU, and datapoint identity.
- Define the shared request, dispatch, acknowledgement, state, status, and alert contracts.
- Identify Flyway ownership and prevent legacy control paths from bypassing monitor locking.
- Fix the simulator device identity, point mapping, state values, and acknowledgement boundary.

**Gate:** Gateway capabilities and gaps are documented, the shared simulator contract is explicit, and no simulator value is presented as a field-device mapping.

**Status:** Complete for the simulator MVP. Evidence is in `docs/IEC104_COMMAND_CONTROL_AUDIT.md`. Fixture: `sim-breaker-1`, `iec104-sim-1`, CA 1, command IOA 150 (`C_DC_NA_1`), feedback IOA 151 (`M_DP_NA_1`), command 1=OFF and 2=ON, feedback 0=TRANSITION, 1=OFF, 2=ON, 3=INVALID. Simulator `ACCEPTED` starts the first timer. PMU `SWITCH` control remains unsupported in the inspected implementation.

### Phase 2 - Add timing configuration

**Purpose:** Store provisional timer values in the database.

**Deliverables:** Flyway table, positive-value constraints, and the `BREAKER_DISCONNECTOR` seed row with 2000 ms and 8000 ms.

**Gate:** A transactional database test reads both values and rejects zero or negative values.

**Status:** Implemented in V15 and verified in a rolled-back PostgreSQL transaction. It is not deployed to a shared database.

### Phase 3 - Persist commands and acquire the device lock

**Purpose:** Guarantee one active command for each physical device.

**Deliverables:** Durable command rows, copied timeout values, unique command IDs, a partial unique device index, and transaction-level device locking.

**Gate:** Two concurrent commands cannot both become active for the same device.

**Status:** Implemented for the MVP. Automated tests cover sequential lock contention; a dedicated concurrent PostgreSQL test is still required before production.

### Phase 4 - Implement basic state transitions

**Purpose:** Handle the command lifecycle without timer recovery concerns.

**Deliverables:** `SENT`, accepted/rejected acknowledgement, initial wait, transition wait, success, communication failure, and timeout transitions.

**Gate:** Unit tests prove every allowed terminal path releases the device lock once.

**Status:** Implemented and covered by the MVP state-machine tests.

### Phase 5 - Add deadline persistence and restart recovery

**Purpose:** Prevent restart from resetting timers or leaving a permanent lock.

**Deliverables:** Absolute persisted deadlines, startup reload, expired-deadline handling, and duplicate-transition protection.

**Gate:** A restart test restores an active deadline without adding time and processes an already-expired deadline immediately.

**Status:** Implemented and unit tested. A live process-restart trace remains outstanding.

### Phase 6 - Build deterministic contract simulator scenarios

**Purpose:** Exercise the monitor before changing the real C gateway.

**Deliverables:** Immediate success, transition then success, no transition, no final state, and rejection scenarios.

**Gate:** Each scenario emits a correlated acknowledgement and deterministic state sequence.

**Status:** Implemented as an MQTT contract simulator. It is not a real IEC-104 server.

### Phase 7 - Add the real IEC-104 acknowledgement adapter

**Purpose:** Connect the C/lib60870 gateway to the shared acknowledgement contract.

**Deliverables:** Handle positive and negative command confirmations, preserve correlation, report dispatch failure correctly, and stop treating the command echo as acknowledgement.

**Gate:** A real IEC-104 simulator produces observable accepted and rejected confirmations through the C gateway.

**Status:** Not started. This is the main protocol gap.

### Phase 8 - Route MQTT commands through the monitor

**Purpose:** Make the monitor the only owner of validation and dispatch.

**Deliverables:** Subscribe to command, acknowledgement, and state topics; publish dispatch, status, and alert messages; block direct legacy bypass.

**Gate:** A command cannot reach a gateway without first acquiring the monitor's device lock.

**Status:** The new monitor topics are implemented and live-tested. Removing or restricting every legacy bypass is still outstanding.

### Phase 9 - Prove the three core outcomes

**Purpose:** Demonstrate the smallest useful end-to-end behavior.

**Scenarios:** Transition then success, no initial change, and transition without final state.

**Gate:** Each scenario has an MQTT trace and a matching durable command row, with zero active locks afterward.

**Status:** Complete against the MQTT contract simulator and isolated PostgreSQL database. Not yet complete through the real IEC-104 gateway.

### Phase 10 - Test rejection, duplication, ordering, and restart

**Purpose:** Prove behavior under delivery and timing failures.

**Scenarios:** Immediate final state, rejection, second active command, duplicate command, duplicate acknowledgement, duplicate state, state before acknowledgement, restart during each timer, and final state after timeout.

**Gate:** Every scenario has an executable test and expected terminal state; no scenario repeats a physical operation or extends a deadline incorrectly.

**Status:** Partially complete. Unit tests cover lock contention, duplicate transition, restart recovery, stale feedback, dispatch failure, and late final feedback. The remaining scenarios need executable integration tests.

### Phase 11 - Add delivery reliability and observability

**Purpose:** Make failures visible and prevent lost terminal events.

**Deliverables:** Transactional outbox or equivalent durable publication, structured logs, and metrics for active commands and terminal outcomes.

**Gate:** A broker outage cannot lose a committed status or alert, and operators can find one command by `commandId`.

**Status:** Not started. MQTT QoS 1 alone does not satisfy this gate.

### Phase 12 - Prepare field-device rollout

**Purpose:** Replace simulator assumptions with verified installation data.

**Deliverables:** Field command/feedback mappings, tested timeout values, acknowledgement policy, configuration guide, alert retention policy, and recovery runbook.

**Gate:** A controlled field trace proves acknowledgement, feedback, success, both timeout failures, and safe recovery without direct database edits.

**Status:** Not started. The 2000 ms and 8000 ms values remain provisional.

## Initial acceptance criteria

The first release is acceptable only when a real command trace demonstrates:

```text
UI command
→ command monitor validation
→ gateway acknowledgement
→ initial timer
→ transition state (optional)
→ completion timer (if transition occurred)
→ final state
→ success status
→ timer and device lock cleared
```

The release must also demonstrate both distinct failures:

```text
no transition within 2 seconds → COMMAND_NO_STATE_CHANGE
transition but no final state within 8 seconds → COMMAND_COMPLETION_TIMEOUT
```

The simulator is a test dependency, not a substitute for gateway/device behavior. The monitor must remain correct when messages are duplicated, delayed, reordered, or lost.
