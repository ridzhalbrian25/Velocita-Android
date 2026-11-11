package main

import (
	"fmt"
	"net/http"
	"sync"
	"time"

	"github.com/gin-gonic/gin"
)

// Section 5.2: Optional Future Sync Specification: Go (Gin) Telemetry Sync Service
// Strictly receives anonymous session summaries (Zero GPS latitude/longitude coordinates).

type TelemetryMetrics struct {
	MaxSpeedKmh         float64 `json:"maxSpeedKmh"`
	AvgSpeedKmh         float64 `json:"avgSpeedKmh"`
	TotalDistanceMeters float64 `json:"totalDistanceMeters"`
}

type TelemetrySessionRequest struct {
	SessionID      string           `json:"sessionId" binding:"required"`
	StartTimestamp int64            `json:"startTimestamp" binding:"required"`
	EndTimestamp   int64            `json:"endTimestamp" binding:"required"`
	Metrics        TelemetryMetrics `json:"metrics" binding:"required"`
	VehicleType    string           `json:"vehicleType" binding:"required"`
}

// Per-client sliding window rate limiter (Max 10 syncs per hour per client)
type RateLimiter struct {
	mu      sync.Mutex
	history map[string][]time.Time
}

func NewRateLimiter() *RateLimiter {
	return &RateLimiter{
		history: make(map[string][]time.Time),
	}
}

func (rl *RateLimiter) Allow(clientKey string) bool {
	rl.mu.Lock()
	defer rl.mu.Unlock()

	now := time.Now()
	cutoff := now.Add(-1 * time.Hour)
	valid := make([]time.Time, 0, 10)
	for _, t := range rl.history[clientKey] {
		if t.After(cutoff) {
			valid = append(valid, t)
		}
	}
	if len(valid) >= 10 {
		rl.history[clientKey] = valid
		return false
	}
	rl.history[clientKey] = append(valid, now)
	return true
}

func main() {
	gin.SetMode(gin.ReleaseMode)
	r := gin.Default()
	limiter := NewRateLimiter()

	r.POST("/api/v1/telemetry/sessions", func(c *gin.Context) {
		clientIP := c.ClientIP()
		if !limiter.Allow(clientIP) {
			c.JSON(http.StatusTooManyRequests, gin.H{
				"status": "FAILED",
				"code":   429,
				"error": gin.H{
					"message": "Rate limit exceeded. Maximum 10 syncs per hour per client.",
				},
			})
			return
		}

		var req TelemetrySessionRequest
		if err := c.ShouldBindJSON(&req); err != nil {
			c.JSON(http.StatusBadRequest, gin.H{
				"status": "FAILED",
				"code":   400,
				"error": gin.H{
					"field":   "payload",
					"message": err.Error(),
				},
			})
			return
		}

		if req.Metrics.MaxSpeedKmh <= 0.0 || req.Metrics.MaxSpeedKmh >= 1200.0 {
			c.JSON(http.StatusBadRequest, gin.H{
				"status": "FAILED",
				"code":   400,
				"error": gin.H{
					"field":   "metrics.maxSpeedKmh",
					"message": "maxSpeedKmh must be a positive float under 1200.0",
				},
			})
			return
		}

		nowUnix := time.Now().Unix()
		recordID := fmt.Sprintf("rec_%d", nowUnix)

		c.JSON(http.StatusCreated, gin.H{
			"status":  "SUCCESS",
			"code":    201,
			"message": "Telemetry session logged successfully.",
			"data": gin.H{
				"recordId":   recordID,
				"serverTime": nowUnix,
			},
		})
	})

	_ = r.Run(":8080")
}
