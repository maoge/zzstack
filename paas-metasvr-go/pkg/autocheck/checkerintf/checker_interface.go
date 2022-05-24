package checkerintf

type CmptProber interface {
	DoCheck(servInstID, servType string) bool
}
