package middleware

import (
	"context"
	"net/http"

	"github.com/go-chi/chi"
)

// ImplementerCtx middleware to extract the ImplementerID from the chi url param and set it into the request context
func ImplementerCtx(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		ImplementerID := chi.URLParam(r, "implementerID")
		ctx := context.WithValue(r.Context(), ContextKeyImplementer, ImplementerID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}
