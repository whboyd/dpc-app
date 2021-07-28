package public

import (
	"github.com/CMSgov/dpc/api/auth"
	"github.com/CMSgov/dpc/api/client"
	"github.com/CMSgov/dpc/api/conf"
	middleware2 "github.com/CMSgov/dpc/api/middleware"
	"github.com/CMSgov/dpc/api/service"
	v2 "github.com/CMSgov/dpc/api/v2"
	"github.com/go-chi/chi"
	"github.com/go-chi/chi/middleware"
	"net/http"
	"strings"
)

func buildPublicRoutes(cont controllers) http.Handler {
	r := chi.NewRouter()
	r.Use(middleware2.Logging())
	r.Use(middleware2.RequestIPCtx)
	fileServer(r, "/v2/swagger", http.Dir("../swaggerui"))
	r.With(middleware2.Sanitize).Route("/v2", func(r chi.Router) {
		r.Use(middleware.SetHeader("Content-Type", "application/fhir+json; charset=UTF-8"))
		r.Get("/metadata", cont.Metadata.Read)

		//ORGANIZATION
		r.Route("/Organization", func(r chi.Router) {
			r.Route("/{organizationID}", func(r chi.Router) {
				r.Use(middleware2.OrganizationCtx)
				r.With(middleware2.FHIRModel).Get("/", cont.Org.Read)
			})
		})

		//GROUP
		r.Route("/Group", func(r chi.Router) {
			r.Use(middleware2.AuthCtx)
			r.With(middleware2.FHIRFilter, middleware2.FHIRModel).Post("/", cont.Group.Create)
			r.Route("/{groupID}", func(r chi.Router) {
				r.Use(middleware2.RequestURLCtx)
				r.Use(middleware2.GroupCtx)
				r.Use(middleware2.ExportTypesParamCtx)
				r.Use(middleware2.ExportSinceParamCtx)
				r.Get("/$export", cont.Group.Export)
			})
		})

		//JOBS
		r.Route("/Jobs", func(r chi.Router) {
			r.Use(middleware.SetHeader("Content-Type", "application/json; charset=UTF-8"))
			r.Use(middleware2.AuthCtx)
			r.With(middleware2.JobCtx).Get("/{jobID}", cont.Job.Status)
		})

		//DATA
		r.Route("/Data", func(r chi.Router) {
			r.Use(middleware2.AuthCtx)
			r.With(middleware2.FileNameCtx).Get("/{fileName}", cont.Data.GetFile)
		})
	})
	r.Post("/auth/token", auth.GetAuthToken)
	return r
}

// NewPublicServer configures clients, builds ADMIN routes, and creates a server.
func NewPublicServer() *service.Server {
	attrClient := client.NewAttributionClient(client.AttributionConfig{
		URL:     conf.GetAsString("attribution-client.url"),
		Retries: conf.GetAsInt("attribution-client.retries", 3),
	})
	dataClient := client.NewDataClient(client.DataConfig{
		URL:     conf.GetAsString("attribution-client.url"),
		Retries: conf.GetAsInt("attribution-client.retries", 3),
	})

	jobClient := client.NewJobClient(client.JobConfig{
		URL:     conf.GetAsString("attribution-client.url"),
		Retries: conf.GetAsInt("attribution-client.retries", 3),
	})

	port := conf.GetAsInt("PUBLIC_PORT", 3000)

	controllers := controllers{
		Org:      v2.NewOrganizationController(attrClient),
		Metadata: v2.NewMetadataController(conf.GetAsString("capabilities.base")),
		Group:    v2.NewGroupController(attrClient),
		Data:     v2.NewDataController(dataClient),
		Job:      v2.NewJobController(jobClient),
	}

	r := buildPublicRoutes(controllers)
	return service.NewServer("DPC-API Public Server", port, true, r)

}

func fileServer(r chi.Router, path string, root http.FileSystem) {
	if strings.ContainsAny(path, "{}*") {
		panic("FileServer does not permit URL parameters.")
	}

	fs := http.StripPrefix(path, http.FileServer(root))

	if path != "/" && path[len(path)-1] != '/' {
		r.Get(path, http.RedirectHandler(path+"/", 301).ServeHTTP)
		path += "/"
	}
	path += "*"

	r.Get(path, func(w http.ResponseWriter, r *http.Request) {
		fs.ServeHTTP(w, r)
	})
}

type controllers struct {
	Org      v2.Controller
	Metadata v2.ReadController
	Group    v2.Controller
	Data     v2.FileController
	Job      v2.JobController
}
