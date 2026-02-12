# Sprint 14-15: ClawPhones Relay Endpoints

## Summary
Added 16 new endpoints to `server.s11-6.js` for Task Distribution (Sprint 14) and Edge Compute Relay (Sprint 15).

All endpoints inserted at **line 1387**, before `server.listen` at line 1994.

## Implementation Details

### Helper Functions Added
- `readJsonFile(path, defaultValue)` - Safe JSON file reading with fallback
- `writeJsonFile(path, data)` - Safe JSON file writing with error handling
- `haversineDistance(lat1, lon1, lat2, lon2)` - Calculate distance between coordinates in km

### Data Files Created (Auto-created on first use)
- `data/tasks.json` - Task storage
- `data/task-results.jsonl` - Task completion log
- `data/compute-nodes.json` - Compute node registry
- `data/compute-jobs.json` - Compute job queue
- `data/compute-results.jsonl` - Job completion log
- `data/push-preferences.json` - Per-node push notification preferences

---

## Sprint 14: Task Distribution (6 endpoints)

### 1. POST /v1/tasks/distribute
**Purpose**: Create and distribute a new task to available nodes

**Request Body**:
```json
{
  "task_id": "optional-custom-id",
  "type": "delivery|surveillance|data_collection",
  "location": { "lat": 37.7749, "lon": -122.4194 },
  "radius_km": 5.0,
  "requirements": { "camera": true, "battery_min": 50 },
  "reward": 100,
  "expires_at": "2026-02-12T00:00:00Z"
}
```

**Response**:
```json
{
  "ok": true,
  "task_id": "task_abc123",
  "h3_cell": "8928308280fffff"
}
```

**Features**:
- Auto-generates task_id if not provided
- Computes H3 cell (resolution 9) from lat/lon
- Sets status to 'open'
- Validates radius_km >= 0.1

---

### 2. GET /v1/tasks/available
**Purpose**: Query available tasks near a location

**Query Parameters**:
- `lat` (required): Latitude
- `lon` (required): Longitude
- `radius` (optional): Search radius in km (default: 10)

**Response**:
```json
{
  "ok": true,
  "tasks": [
    {
      "task_id": "task_abc123",
      "type": "delivery",
      "location": { "lat": 37.7749, "lon": -122.4194 },
      "radius_km": 5.0,
      "status": "open",
      "distance_km": 2.34,
      ...
    }
  ]
}
```

**Features**:
- Filters by status='open'
- Auto-expires tasks past expires_at
- Uses Haversine distance calculation
- Sorts by distance (closest first)

---

### 3. POST /v1/tasks/:id/claim
**Purpose**: Claim an available task

**Request Body**:
```json
{
  "node_id": "node_xyz789"
}
```

**Response**:
```json
{
  "ok": true,
  "task": {
    "task_id": "task_abc123",
    "status": "claimed",
    "claimed_by": "node_xyz789",
    "claimed_at": "2026-02-11T20:00:00Z",
    ...
  }
}
```

**Error Responses**:
- 404: Task not found
- 409: Task already claimed/completed

---

### 4. POST /v1/tasks/:id/heartbeat
**Purpose**: Update task progress and keep-alive

**Request Body**:
```json
{
  "progress_pct": 45
}
```

**Response**:
```json
{
  "ok": true,
  "task_id": "task_abc123",
  "progress_pct": 45
}
```

**Features**:
- Updates `last_heartbeat` timestamp
- Validates progress_pct in range 0-100

---

### 5. POST /v1/tasks/:id/results
**Purpose**: Submit task completion results

**Request Body**:
```json
{
  "results": {
    "photos_taken": 12,
    "area_covered_m2": 5000,
    "anomalies_detected": 3
  }
}
```

**Response**:
```json
{
  "ok": true,
  "task_id": "task_abc123",
  "status": "completed"
}
```

**Features**:
- Sets status to 'completed'
- Appends to `data/task-results.jsonl` log
- Records completion timestamp

---

### 6. GET /v1/tasks/stats
**Purpose**: Get task system statistics

**Response**:
```json
{
  "ok": true,
  "stats": {
    "open": 12,
    "claimed": 5,
    "completed": 143,
    "expired": 8,
    "total": 168
  }
}
```

---

## Sprint 15: Edge Compute Relay (8 endpoints)

### 7. POST /v1/compute/nodes/register
**Purpose**: Register a compute node with capabilities

**Request Body**:
```json
{
  "node_id": "optional-custom-id",
  "capabilities": ["gpu", "large-memory", "video-processing"]
}
```

**Response**:
```json
{
  "ok": true,
  "node_id": "compute_def456",
  "capabilities": ["gpu", "large-memory", "video-processing"]
}
```

**Features**:
- Auto-generates node_id if not provided
- Sets status to 'online'
- Records registration and last_heartbeat timestamps

---

### 8. GET /v1/compute/jobs/poll
**Purpose**: Poll for available compute jobs matching node capabilities

**Query Parameters**:
- `node_id` (required): Compute node identifier

**Response**:
```json
{
  "ok": true,
  "job": {
    "job_id": "job_ghi789",
    "type": "video-inference",
    "requirements": ["gpu"],
    "input_data": { "video_url": "..." },
    "priority": 8,
    "reward": 50
  }
}
```

or if no matching job:
```json
{
  "ok": true,
  "job": null
}
```

**Features**:
- Matches job requirements with node capabilities
- Returns first matching pending job
- Returns null if no jobs available

---

### 9. POST /v1/compute/jobs/:id/claim
**Purpose**: Claim a compute job

**Request Body**:
```json
{
  "node_id": "compute_def456"
}
```

**Response**:
```json
{
  "ok": true,
  "job": {
    "job_id": "job_ghi789",
    "status": "claimed",
    "claimed_by": "compute_def456",
    "claimed_at": "2026-02-11T20:05:00Z",
    ...
  }
}
```

**Error Responses**:
- 404: Job not found
- 409: Job already claimed/completed

---

### 10. POST /v1/compute/jobs/:id/heartbeat
**Purpose**: Update job progress

**Request Body**:
```json
{
  "progress_pct": 67
}
```

**Response**:
```json
{
  "ok": true,
  "job_id": "job_ghi789",
  "progress_pct": 67
}
```

---

### 11. POST /v1/compute/jobs/:id/results
**Purpose**: Submit job completion results

**Request Body**:
```json
{
  "results": {
    "output_url": "https://...",
    "processing_time_ms": 4523,
    "frames_processed": 1200
  }
}
```

**Response**:
```json
{
  "ok": true,
  "job_id": "job_ghi789",
  "status": "completed"
}
```

**Features**:
- Sets status to 'completed'
- Appends to `data/compute-results.jsonl` log

---

### 12. GET /v1/compute/nodes/online
**Purpose**: Get list of online compute nodes

**Response**:
```json
{
  "ok": true,
  "nodes": [
    {
      "node_id": "compute_def456",
      "capabilities": ["gpu", "large-memory"],
      "status": "online",
      "last_heartbeat": "2026-02-11T20:10:00Z",
      ...
    }
  ],
  "count": 1
}
```

**Features**:
- Filters nodes with last_heartbeat within 5 minutes
- Returns full node details

---

### 13. POST /v1/compute/jobs
**Purpose**: Create a new compute job

**Request Body**:
```json
{
  "type": "video-inference|image-processing|model-training",
  "requirements": ["gpu", "large-memory"],
  "input_data": { "url": "...", "params": {} },
  "priority": 8,
  "reward": 50
}
```

**Response**:
```json
{
  "ok": true,
  "job_id": "job_ghi789",
  "status": "pending"
}
```

**Features**:
- Auto-generates job_id
- Default priority: 5
- Default reward: 0

---

### 14. GET /v1/compute/stats
**Purpose**: Get compute system statistics

**Response**:
```json
{
  "ok": true,
  "stats": {
    "pending": 8,
    "claimed": 3,
    "completed": 245,
    "failed": 2,
    "total": 258,
    "online_nodes": 4,
    "total_nodes": 7
  }
}
```

**Features**:
- Counts jobs by status
- Reports online nodes (last_heartbeat < 5min)

---

## Push Preferences (2 endpoints)

### 15. GET /v1/push/preferences
**Purpose**: Get push notification preferences for a node

**Query Parameters**:
- `node_id` (required): Node identifier

**Response**:
```json
{
  "ok": true,
  "preferences": {
    "node_id": "node_xyz789",
    "enabled": true,
    "vision_events": true,
    "community_alerts": true,
    "task_updates": true,
    "compute_jobs": true
  }
}
```

**Features**:
- Returns defaults if preferences not set
- All preferences default to `true`

---

### 16. PUT /v1/push/preferences
**Purpose**: Update push notification preferences

**Request Body**:
```json
{
  "node_id": "node_xyz789",
  "enabled": true,
  "vision_events": false,
  "community_alerts": true,
  "task_updates": true,
  "compute_jobs": false
}
```

**Response**:
```json
{
  "ok": true,
  "preferences": {
    "node_id": "node_xyz789",
    "enabled": true,
    "vision_events": false,
    "community_alerts": true,
    "task_updates": true,
    "compute_jobs": false,
    "updated_at": "2026-02-11T20:15:00Z"
  }
}
```

**Features**:
- Partial updates supported (only provided fields updated)
- Preserves existing preferences for omitted fields
- Records `updated_at` timestamp

---

## Error Handling

All endpoints include comprehensive error handling:
- 400: Invalid JSON, missing required fields, invalid parameters
- 404: Resource not found (task/job/node)
- 409: Conflict (task/job already claimed)
- 500: Internal server error (file I/O failure)

All errors return consistent format:
```json
{
  "ok": false,
  "error": "description of error"
}
```

---

## Testing Recommendations

### Task Distribution Flow
1. POST /v1/tasks/distribute (create task)
2. GET /v1/tasks/available (find nearby tasks)
3. POST /v1/tasks/:id/claim (claim task)
4. POST /v1/tasks/:id/heartbeat (update progress)
5. POST /v1/tasks/:id/results (complete task)
6. GET /v1/tasks/stats (verify statistics)

### Compute Flow
1. POST /v1/compute/nodes/register (register node)
2. POST /v1/compute/jobs (create job)
3. GET /v1/compute/jobs/poll (node polls for job)
4. POST /v1/compute/jobs/:id/claim (claim job)
5. POST /v1/compute/jobs/:id/heartbeat (report progress)
6. POST /v1/compute/jobs/:id/results (submit results)
7. GET /v1/compute/nodes/online (verify node online)
8. GET /v1/compute/stats (check system stats)

### Push Preferences Flow
1. GET /v1/push/preferences?node_id=X (get current prefs)
2. PUT /v1/push/preferences (update prefs)
3. GET /v1/push/preferences?node_id=X (verify changes)

---

## File Status
- **File**: `~/.openclaw/workspace/server.s11-6.js`
- **Total Lines**: 1997
- **New Code**: Lines 1387-1991 (605 lines)
- **Syntax**: ✓ PASSED
- **Ready for**: Testing, integration, deployment

