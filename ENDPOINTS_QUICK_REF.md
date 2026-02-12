# ClawPhones Relay API - Quick Reference

## Sprint 14: Task Distribution
```
POST   /v1/tasks/distribute        Create new task
GET    /v1/tasks/available         Find tasks near location (?lat=X&lon=Y&radius=Z)
POST   /v1/tasks/:id/claim         Claim a task
POST   /v1/tasks/:id/heartbeat     Update task progress
POST   /v1/tasks/:id/results       Submit task results
GET    /v1/tasks/stats             Task statistics
```

## Sprint 15: Edge Compute Relay
```
POST   /v1/compute/nodes/register  Register compute node
GET    /v1/compute/jobs/poll       Poll for jobs (?node_id=X)
POST   /v1/compute/jobs/:id/claim  Claim a compute job
POST   /v1/compute/jobs/:id/heartbeat  Update job progress
POST   /v1/compute/jobs/:id/results    Submit job results
GET    /v1/compute/nodes/online    List online compute nodes
POST   /v1/compute/jobs            Create new compute job
GET    /v1/compute/stats           Compute system statistics
```

## Push Preferences
```
GET    /v1/push/preferences        Get preferences (?node_id=X)
PUT    /v1/push/preferences        Update preferences
```

## Data Files (auto-created in data/)
- tasks.json, task-results.jsonl
- compute-nodes.json, compute-jobs.json, compute-results.jsonl
- push-preferences.json

## Testing
```bash
# Start server
cd ~/.openclaw/workspace
node server.s11-6.js

# Test task distribution
curl -X POST http://localhost:8787/v1/tasks/distribute \
  -H "Content-Type: application/json" \
  -d '{"type":"delivery","location":{"lat":37.7749,"lon":-122.4194},"radius_km":5,"reward":100}'

# Test compute node registration
curl -X POST http://localhost:8787/v1/compute/nodes/register \
  -H "Content-Type: application/json" \
  -d '{"capabilities":["gpu","large-memory"]}'
```
