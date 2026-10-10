extends SceneTree

# Run with: godot --headless --path . --script res://tests/smoke_test.gd

func _initialize() -> void:
    call_deferred("_run_checks")

func _run_checks() -> void:
    var packed := load("res://main.tscn") as PackedScene
    if packed == null:
        push_error("Cannot load main.tscn")
        quit(1)
        return

    var scene := packed.instantiate()
    if scene == null:
        push_error("Cannot instantiate main scene")
        quit(1)
        return

    root.add_child(scene)

    if scene.get_node_or_null("Player") == null:
        push_error("Missing Player node")
        quit(1)
        return
    if scene.get_node_or_null("HUD") == null:
        push_error("Missing HUD node")
        quit(1)
        return

    var state: Dictionary = scene.get("state")
    if not state.has("day") or not state.has("energy") or not state.has("videos"):
        push_error("Gameplay state is missing required keys")
        quit(1)
        return

    print("Creator Life 3D smoke checks passed.")
    quit(0)
