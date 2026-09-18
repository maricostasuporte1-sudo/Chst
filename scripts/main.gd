extends Node3D

const SAVE_PATH := "user://creator_life_save.json"
const MOVE_SPEED := 3.6
const MOUSE_SENS := 0.0024
const TOUCH_SENS := 0.0030
const INTERACT_DISTANCE := 3.0

var state := {
    "day": 1,
    "money": 50.0,
    "followers": 0,
    "views": 0,
    "energy": 100,
    "pc_level": 1,
    "internet_level": 1,
    "videos": 0
}

var player: CharacterBody3D
var head: Node3D
var camera: Camera3D
var hud_stats: Label
var prompt_label: Label
var toast_label: Label
var pc_panel: PanelContainer
var pc_info: Label
var rng := RandomNumberGenerator.new()

var touch_forward := false
var touch_back := false
var touch_left := false
var touch_right := false
var look_pitch := 0.0
var toast_token := 0

func _ready() -> void:
    rng.randomize()
    _load_game()
    _build_environment()
    _build_room()
    _build_player()
    _build_hud()
    _update_hud()
    Input.mouse_mode = Input.MOUSE_MODE_CAPTURED

func _build_environment() -> void:
    var env := WorldEnvironment.new()
    var e := Environment.new()
    e.background_mode = Environment.BG_COLOR
    e.background_color = Color("#8fb4cf")
    e.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
    e.ambient_light_color = Color("#d9e7f2")
    e.ambient_light_energy = 0.65
    env.environment = e
    add_child(env)

    var sun := DirectionalLight3D.new()
    sun.rotation_degrees = Vector3(-55, -25, 0)
    sun.light_energy = 1.0
    sun.shadow_enabled = true
    add_child(sun)

    var lamp := OmniLight3D.new()
    lamp.position = Vector3(0, 2.7, 0)
    lamp.light_energy = 4.0
    lamp.omni_range = 8.0
    add_child(lamp)

func _make_box(name_: String, pos: Vector3, size: Vector3, color: Color, collision := true, kind := "") -> Node3D:
    var root: Node3D
    if collision:
        var body := StaticBody3D.new()
        root = body
        var shape_node := CollisionShape3D.new()
        var shape := BoxShape3D.new()
        shape.size = size
        shape_node.shape = shape
        body.add_child(shape_node)
        if kind != "":
            body.set_meta("kind", kind)
    else:
        root = Node3D.new()

    root.name = name_
    root.position = pos

    var mesh_instance := MeshInstance3D.new()
    var mesh := BoxMesh.new()
    mesh.size = size
    mesh_instance.mesh = mesh
    var mat := StandardMaterial3D.new()
    mat.albedo_color = color
    mat.roughness = 0.82
    mesh_instance.material_override = mat
    root.add_child(mesh_instance)
    add_child(root)
    return root

func _build_room() -> void:
    _make_box("Floor", Vector3(0, -0.1, 0), Vector3(8, 0.2, 7), Color("#b98f66"))
    _make_box("BackWall", Vector3(0, 1.5, -3.45), Vector3(8, 3.0, 0.1), Color("#ece8df"))
    _make_box("LeftWall", Vector3(-3.95, 1.5, 0), Vector3(0.1, 3.0, 7), Color("#e2ddd5"))
    _make_box("RightWall", Vector3(3.95, 1.5, 0), Vector3(0.1, 3.0, 7), Color("#e2ddd5"))
    _make_box("FrontWall", Vector3(0, 1.5, 3.45), Vector3(8, 3.0, 0.1), Color("#e9e4da"))

    _make_box("DeskTop", Vector3(0, 0.78, -2.55), Vector3(3.0, 0.12, 0.8), Color("#5b4636"))
    _make_box("DeskLegL", Vector3(-1.25, 0.38, -2.55), Vector3(0.12, 0.75, 0.7), Color("#4b392d"))
    _make_box("DeskLegR", Vector3(1.25, 0.38, -2.55), Vector3(0.12, 0.75, 0.7), Color("#4b392d"))
    var pc := _make_box("OldPC", Vector3(0, 1.24, -2.67), Vector3(0.85, 0.58, 0.18), Color("#23272d"), true, "pc")
    pc.set_meta("label", "PC antigo")
    _make_box("MonitorGlow", Vector3(0, 1.24, -2.565), Vector3(0.68, 0.40, 0.025), Color("#6ba8c7"), false)
    _make_box("PCBox", Vector3(1.0, 0.42, -2.56), Vector3(0.42, 0.72, 0.55), Color("#34383f"))
    _make_box("Chair", Vector3(0, 0.48, -1.55), Vector3(0.65, 0.75, 0.65), Color("#66574e"))

    var bed := _make_box("Bed", Vector3(-2.45, 0.32, 0.65), Vector3(1.45, 0.55, 2.45), Color("#5e7ea3"), true, "bed")
    bed.set_meta("label", "cama")
    _make_box("Pillow", Vector3(-2.45, 0.65, -0.08), Vector3(1.05, 0.18, 0.50), Color("#f0eee8"), false)

    _make_box("Shelf", Vector3(2.8, 1.05, -2.9), Vector3(1.0, 1.9, 0.35), Color("#785a43"))
    _make_box("FoodBox", Vector3(2.75, 0.25, 1.9), Vector3(0.75, 0.5, 0.75), Color("#d3a85f"))

func _build_player() -> void:
    player = CharacterBody3D.new()
    player.name = "Player"
    player.position = Vector3(0, 0.9, 1.75)

    var collider := CollisionShape3D.new()
    var capsule := CapsuleShape3D.new()
    capsule.radius = 0.34
    capsule.height = 1.55
    collider.shape = capsule
    player.add_child(collider)

    head = Node3D.new()
    head.name = "Head"
    head.position = Vector3(0, 0.55, 0)
    player.add_child(head)

    camera = Camera3D.new()
    camera.current = true
    camera.fov = 72.0
    head.add_child(camera)
    add_child(player)

func _build_hud() -> void:
    var canvas := CanvasLayer.new()
    canvas.name = "HUD"
    add_child(canvas)

    hud_stats = Label.new()
    hud_stats.position = Vector2(18, 14)
    hud_stats.add_theme_font_size_override("font_size", 23)
    hud_stats.add_theme_color_override("font_color", Color.WHITE)
    hud_stats.add_theme_color_override("font_shadow_color", Color(0, 0, 0, 0.8))
    hud_stats.add_theme_constant_override("shadow_offset_x", 2)
    hud_stats.add_theme_constant_override("shadow_offset_y", 2)
    canvas.add_child(hud_stats)

    var crosshair := Label.new()
    crosshair.text = "+"
    crosshair.add_theme_font_size_override("font_size", 26)
    crosshair.set_anchors_preset(Control.PRESET_CENTER)
    crosshair.position = Vector2(-8, -18)
    canvas.add_child(crosshair)

    prompt_label = Label.new()
    prompt_label.text = ""
    prompt_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
    prompt_label.add_theme_font_size_override("font_size", 22)
    prompt_label.set_anchors_and_offsets_preset(Control.PRESET_CENTER_BOTTOM)
    prompt_label.position = Vector2(-260, -95)
    prompt_label.size = Vector2(520, 40)
    canvas.add_child(prompt_label)

    toast_label = Label.new()
    toast_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
    toast_label.add_theme_font_size_override("font_size", 22)
    toast_label.set_anchors_and_offsets_preset(Control.PRESET_CENTER_TOP)
    toast_label.position = Vector2(-320, 70)
    toast_label.size = Vector2(640, 70)
    canvas.add_child(toast_label)

    _build_mobile_controls(canvas)
    _build_pc_panel(canvas)

func _mk_btn(text_: String, pos: Vector2, size_: Vector2) -> Button:
    var b := Button.new()
    b.text = text_
    b.position = pos
    b.size = size_
    b.add_theme_font_size_override("font_size", 23)
    return b

func _build_mobile_controls(canvas: CanvasLayer) -> void:
    var controls := Control.new()
    controls.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    controls.mouse_filter = Control.MOUSE_FILTER_PASS
    canvas.add_child(controls)

    var viewport_size := Vector2(1280, 720)
    var up := _mk_btn("▲", Vector2(105, viewport_size.y - 190), Vector2(76, 66))
    var left := _mk_btn("◀", Vector2(25, viewport_size.y - 120), Vector2(76, 66))
    var down := _mk_btn("▼", Vector2(105, viewport_size.y - 120), Vector2(76, 66))
    var right := _mk_btn("▶", Vector2(185, viewport_size.y - 120), Vector2(76, 66))
    var act := _mk_btn("AÇÃO", Vector2(viewport_size.x - 175, viewport_size.y - 130), Vector2(145, 82))

    for b in [up, left, down, right, act]:
        controls.add_child(b)

    up.button_down.connect(func(): touch_forward = true)
    up.button_up.connect(func(): touch_forward = false)
    down.button_down.connect(func(): touch_back = true)
    down.button_up.connect(func(): touch_back = false)
    left.button_down.connect(func(): touch_left = true)
    left.button_up.connect(func(): touch_left = false)
    right.button_down.connect(func(): touch_right = true)
    right.button_up.connect(func(): touch_right = false)
    act.pressed.connect(_interact)

func _build_pc_panel(canvas: CanvasLayer) -> void:
    pc_panel = PanelContainer.new()
    pc_panel.visible = false
    pc_panel.set_anchors_preset(Control.PRESET_CENTER)
    pc_panel.position = Vector2(-260, -220)
    pc_panel.size = Vector2(520, 440)
    canvas.add_child(pc_panel)

    var vb := VBoxContainer.new()
    vb.add_theme_constant_override("separation", 14)
    pc_panel.add_child(vb)

    var title := Label.new()
    title.text = "MEU PC - CREATOR LIFE"
    title.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
    title.add_theme_font_size_override("font_size", 27)
    vb.add_child(title)

    pc_info = Label.new()
    pc_info.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
    pc_info.add_theme_font_size_override("font_size", 20)
    pc_info.custom_minimum_size = Vector2(480, 110)
    vb.add_child(pc_info)

    var create_btn := Button.new()
    create_btn.text = "GRAVAR E PUBLICAR VIDEO  (-20 energia)"
    create_btn.custom_minimum_size = Vector2(470, 55)
    create_btn.pressed.connect(_create_video)
    vb.add_child(create_btn)

    var pc_up := Button.new()
    pc_up.text = "MELHORAR PC  (R$ 150 x nivel)"
    pc_up.custom_minimum_size = Vector2(470, 48)
    pc_up.pressed.connect(_upgrade_pc)
    vb.add_child(pc_up)

    var net_up := Button.new()
    net_up.text = "MELHORAR INTERNET  (R$ 120 x nivel)"
    net_up.custom_minimum_size = Vector2(470, 48)
    net_up.pressed.connect(_upgrade_internet)
    vb.add_child(net_up)

    var close_btn := Button.new()
    close_btn.text = "FECHAR"
    close_btn.custom_minimum_size = Vector2(470, 45)
    close_btn.pressed.connect(_close_pc)
    vb.add_child(close_btn)

func _physics_process(delta: float) -> void:
    if pc_panel != null and pc_panel.visible:
        player.velocity = Vector3.ZERO
        return

    var input_x := Input.get_axis("move_left", "move_right")
    var input_z := Input.get_axis("move_forward", "move_back")
    if touch_left:
        input_x -= 1.0
    if touch_right:
        input_x += 1.0
    if touch_forward:
        input_z -= 1.0
    if touch_back:
        input_z += 1.0

    var input_vec := Vector2(input_x, input_z).normalized()
    var forward := -player.global_transform.basis.z
    var right := player.global_transform.basis.x
    var direction := right * input_vec.x + forward * -input_vec.y
    direction.y = 0.0
    direction = direction.normalized()

    player.velocity.x = direction.x * MOVE_SPEED
    player.velocity.z = direction.z * MOVE_SPEED
    if not player.is_on_floor():
        player.velocity.y -= 18.0 * delta
    else:
        player.velocity.y = 0.0
    player.move_and_slide()
    _update_prompt()

func _unhandled_input(event: InputEvent) -> void:
    if event is InputEventKey and event.keycode == KEY_ESCAPE and event.pressed:
        if pc_panel.visible:
            _close_pc()
        else:
            Input.mouse_mode = Input.MOUSE_MODE_VISIBLE if Input.mouse_mode == Input.MOUSE_MODE_CAPTURED else Input.MOUSE_MODE_CAPTURED

    if event.is_action_pressed("interact") and not pc_panel.visible:
        _interact()

    if event is InputEventMouseMotion and Input.mouse_mode == Input.MOUSE_MODE_CAPTURED and not pc_panel.visible:
        _apply_look(event.relative.x * MOUSE_SENS, event.relative.y * MOUSE_SENS)

    if event is InputEventScreenDrag and not pc_panel.visible:
        var w := get_viewport().get_visible_rect().size.x
        if event.position.x > w * 0.42:
            _apply_look(event.relative.x * TOUCH_SENS, event.relative.y * TOUCH_SENS)

func _apply_look(dx: float, dy: float) -> void:
    player.rotate_y(-dx)
    look_pitch = clamp(look_pitch - dy, deg_to_rad(-78), deg_to_rad(78))
    head.rotation.x = look_pitch

func _ray_hit() -> Dictionary:
    if camera == null:
        return {}
    var from := camera.global_position
    var to := from + (-camera.global_transform.basis.z * INTERACT_DISTANCE)
    var query := PhysicsRayQueryParameters3D.create(from, to)
    query.exclude = [player.get_rid()]
    return get_world_3d().direct_space_state.intersect_ray(query)

func _update_prompt() -> void:
    if pc_panel.visible:
        prompt_label.text = ""
        return
    var hit := _ray_hit()
    if hit.is_empty():
        prompt_label.text = ""
        return
    var obj = hit.get("collider")
    if obj != null and obj.has_meta("kind"):
        var label_text: String = str(obj.get_meta("label", "objeto"))
        prompt_label.text = "E / ACAO - usar %s" % label_text
    else:
        prompt_label.text = ""

func _interact() -> void:
    if pc_panel.visible:
        return
    var hit := _ray_hit()
    if hit.is_empty():
        return
    var obj = hit.get("collider")
    if obj == null or not obj.has_meta("kind"):
        return
    match str(obj.get_meta("kind")):
        "pc":
            _open_pc()
        "bed":
            _sleep()

func _open_pc() -> void:
    pc_panel.visible = true
    Input.mouse_mode = Input.MOUSE_MODE_VISIBLE
    _refresh_pc_info()

func _close_pc() -> void:
    pc_panel.visible = false
    Input.mouse_mode = Input.MOUSE_MODE_CAPTURED

func _refresh_pc_info() -> void:
    var upload_minutes := max(1, 210 / int(state["internet_level"]))
    pc_info.text = "PC nivel %d   -   Internet nivel %d\nUpload estimado: %d min do jogo\nVideos publicados: %d" % [int(state["pc_level"]), int(state["internet_level"]), upload_minutes, int(state["videos"])]

func _create_video() -> void:
    if int(state["energy"]) < 20:
        _toast("Energia baixa. Durma para recuperar.")
        return

    state["energy"] = int(state["energy"]) - 20
    var pc_level := int(state["pc_level"])
    var internet_level := int(state["internet_level"])
    var followers := int(state["followers"])

    var base := rng.randi_range(60, 150)
    var growth := int(followers * rng.randf_range(0.10, 0.24))
    var quality := pc_level * rng.randi_range(30, 70) + internet_level * rng.randi_range(10, 35)
    var video_views := max(25, base + growth + quality)

    if rng.randf() < 0.08:
        video_views *= rng.randi_range(3, 7)

    var gained_followers := max(1, int(video_views * rng.randf_range(0.025, 0.065)))
    var earned := float(video_views) * 0.012

    state["views"] = int(state["views"]) + video_views
    state["followers"] = int(state["followers"]) + gained_followers
    state["money"] = float(state["money"]) + earned
    state["videos"] = int(state["videos"]) + 1

    _save_game()
    _update_hud()
    _refresh_pc_info()
    _toast("Video publicado: %d views  -  +%d seguidores  -  R$ %.2f" % [video_views, gained_followers, earned])

func _upgrade_pc() -> void:
    var level := int(state["pc_level"])
    var cost := 150.0 * level
    if float(state["money"]) < cost:
        _toast("Faltam R$ %.2f para melhorar o PC." % (cost - float(state["money"])))
        return
    state["money"] = float(state["money"]) - cost
    state["pc_level"] = level + 1
    _save_game()
    _update_hud()
    _refresh_pc_info()
    _toast("PC melhorado para o nivel %d." % int(state["pc_level"]))

func _upgrade_internet() -> void:
    var level := int(state["internet_level"])
    var cost := 120.0 * level
    if float(state["money"]) < cost:
        _toast("Faltam R$ %.2f para melhorar a internet." % (cost - float(state["money"])))
        return
    state["money"] = float(state["money"]) - cost
    state["internet_level"] = level + 1
    _save_game()
    _update_hud()
    _refresh_pc_info()
    _toast("Internet melhorada para o nivel %d." % int(state["internet_level"]))

func _sleep() -> void:
    state["day"] = int(state["day"]) + 1
    state["energy"] = 100
    _save_game()
    _update_hud()
    _toast("Novo dia! Energia recuperada.")

func _update_hud() -> void:
    hud_stats.text = "DIA %d  |  R$ %.2f  |  Seguidores %d  |  Views %d  |  Energia %d" % [int(state["day"]), float(state["money"]), int(state["followers"]), int(state["views"]), int(state["energy"])]

func _toast(text_: String) -> void:
    toast_token += 1
    var my_token := toast_token
    toast_label.text = text_
    await get_tree().create_timer(3.2).timeout
    if my_token == toast_token:
        toast_label.text = ""

func _save_game() -> void:
    var file := FileAccess.open(SAVE_PATH, FileAccess.WRITE)
    if file:
        file.store_string(JSON.stringify(state))

func _load_game() -> void:
    if not FileAccess.file_exists(SAVE_PATH):
        return
    var file := FileAccess.open(SAVE_PATH, FileAccess.READ)
    if file == null:
        return
    var parsed = JSON.parse_string(file.get_as_text())
    if parsed is Dictionary:
        for key in state.keys():
            if parsed.has(key):
                state[key] = parsed[key]

func _notification(what: int) -> void:
    if what == NOTIFICATION_WM_CLOSE_REQUEST:
        _save_game()
        get_tree().quit()
