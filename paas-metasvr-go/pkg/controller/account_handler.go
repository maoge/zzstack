package controller

import (
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/gin-gonic/gin/binding"
	"github.com/maoge/paas-metasvr-go/pkg/meta"
	"github.com/maoge/paas-metasvr-go/pkg/meta/proto"
)

type AccountHandler struct {
	group *gin.RouterGroup
}

func NewAccountHandler(g *gin.RouterGroup) AccountHandler {
	return AccountHandler{group: g}
}

func (m *AccountHandler) Login() {
	m.group.POST("/login", func(c *gin.Context) {
		// bodyAsByteArray, err := ioutil.ReadAll(c.Request.Body)
		// if err != nil {
		// 	c.JSON(200, gin.H{"ret_code": "-1", "ret_info": "bad request"})
		// } else {
		// 	// utils.LOGGER.Info(fmt.Sprintf("%v", string(jsonData)))
		// 	//jsonBody := string(bodyAsByteArray)
		// 	c.JSON(200, jsonBody)
		// }

		var user proto.AccUser
		err := c.MustBindWith(&user, binding.JSON)
		if err == nil {
			resultBean := proto.NewResultBean()
			meta.Login(&user, resultBean)
			c.JSON(http.StatusOK, user)
		} else {
			c.String(http.StatusBadRequest, "参数绑定失败"+err.Error())
		}

	})
}

// @Service(id = "login", method = HttpMethodEnum.POST, auth = false, bwswitch = false, bodyParams = {
// 	@Parameter(name = FixHeader.HEADER_USER, type = ParamType.ParamString, required = true),
// 	@Parameter(name = FixHeader.HEADER_PASSWORD, type = ParamType.ParamString, required = true) })
// public static void login(RoutingContext ctx) {
// RequestParameters params = HttpUtils.getValidateParams(ctx);
// RequestParameter body = params.body();
// JsonObject bodyJson = body.getJsonObject();

// String user = bodyJson.getString(FixHeader.HEADER_USER);
// String passwd = bodyJson.getString(FixHeader.HEADER_PASSWORD);

// ResultBean result = new ResultBean();
// MetaDataDao.login(user, passwd, result);

// HttpUtils.outResultBean(ctx, result);
// }
