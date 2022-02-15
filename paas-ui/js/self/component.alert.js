var Component = window.Component || {};


(function(Component) {
	
	function Alert(type, content) {
		return Util.alert(type,content);
	}

	Component.Alert = Alert;

})(Component);
