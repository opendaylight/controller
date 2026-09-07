## Purpose

This PR records the OpenDaylight portion of an academic differential-testing
disclosure set. The historical 2023 record classified DC-017 and DC-023 as
previously undocumented on the evaluated Carbon-era target. That historical
label is not maintainer confirmation, a CVE, or a claim that the current
implementation is vulnerable.

The entries are requests for validation. The current replay level `L1` means
that the stated differential signal was reproduced in the local harness. Raw
OpenFlow payloads, relay automation, and the full experiment corpus are not
included.

## Tested target

- Project: `opendaylight/controller` and its OpenFlow plugin components
- Historical target: OpenDaylight Carbon 0.6.2, OpenFlow 1.3, isolated
  Mininet/OVS harness
- Artifact and methodology: <https://github.com/Drone-Lab/DiffController>

## Candidate records

| ID | Finding | Message/action | Current replay | Current boundary |
|---|---|---|---|---|
| DC-017 | Hello after channel establishment | `of_hello`; BUILD | L1 | known/public state-machine class; not claimed as a new 0-day |
| DC-023 | Inconsistent switch-supplied flow entry exposed in northbound inventory | `of_flow_stats_reply`; ADD; one flow-stat entry | L1 | narrow inventory-integrity signal; no claim of flow installation, persistence, poisoning, DoS, or data-plane impact |

For DC-023, the archived OpenDaylight replay observed the exact sentinel in
11/12 operational snapshots after 12/12 strict same-XID injections and in 0/10
pass-through controls. Channels remained open and REST returned HTTP 200 in the
primary cases. The OpenFlow specification does not explicitly require
controller-side revalidation of every switch statistics reply, so this report
asks maintainers to determine the intended trust and provenance boundary.

## Requested action

Please confirm the affected component and versions, whether these behaviors
are expected, and whether an existing Gerrit/GitHub issue, advisory, or fix
tracks either row. We can provide a minimal redacted reproducer, paired-control
logs, and hashes through a maintainer-selected channel. The OpenDaylight
project may prefer Gerrit for development; this PR is the public GitHub record
requested for the disclosure.
