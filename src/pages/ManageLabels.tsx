import { useState } from 'react';
import { useNavigate } from 'react-router';
import { ArrowLeft, Plus, Tag, Trash2, Pencil } from 'lucide-react';
import { toast } from 'sonner';
import { useFinanceStore } from '@/store/useFinanceStore';
import { COLOR_PALETTE } from '@/data/colorPalette';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { useConfirm } from '@/components/ui/use-confirm';
import Header from '@/components/ui/header';
import { HeaderIconButton } from '@/components/ui/header-icon-button';
import Main from '@/components/ui/main';
import { MAX_NAME_LENGTH, cleanText, stripLeading } from '@/utils/validation';

const labelColors = COLOR_PALETTE;

export default function ManageLabels() {
  const navigate = useNavigate();
  const confirm = useConfirm();
  const labels = useFinanceStore((s) => s.labels);
  const addLabel = useFinanceStore((s) => s.addLabel);
  const updateLabel = useFinanceStore((s) => s.updateLabel);
  const deleteLabel = useFinanceStore((s) => s.deleteLabel);

  const [open, setOpen] = useState(false);
  const [editId, setEditId] = useState<string | null>(null);
  const [name, setName] = useState('');
  const [color, setColor] = useState(labelColors[0]);

  const handleEdit = (id: string) => {
    const label = labels.find((l) => l.id === id);
    if (!label) return;
    setEditId(id);
    setName(label.name);
    setColor(label.color);
    setOpen(true);
  };

  const handleSubmit = () => {
    const cleanName = cleanText(name, MAX_NAME_LENGTH);
    if (!cleanName) {
      toast.error('Enter a name');
      return;
    }
    // Same rule as categories: names are compared case-insensitively, so "essential" can't
    // sit beside "Essential" and leave the label picker with two indistinguishable chips.
    const key = cleanName.toLowerCase();
    if (labels.some((l) => l.id !== editId && l.name.trim().toLowerCase() === key)) {
      toast.error(`A label named "${cleanName}" already exists`);
      return;
    }
    if (editId) {
      updateLabel(editId, { name: cleanName, color });
      toast.success('Label updated');
    } else {
      addLabel({ name: cleanName, color });
      toast.success('Label added');
    }
    resetForm();
  };

  const resetForm = () => {
    setOpen(false);
    setEditId(null);
    setName('');
    setColor(labelColors[0]);
  };

  return (
    <>
      {/* Header */}
      <Header innerClassName="lg:max-w-xl">
        <HeaderIconButton onClick={() => navigate(-1)} aria-label="Back">
          <ArrowLeft />
        </HeaderIconButton>
        <h1 className="text-base font-semibold">Labels</h1>
        <HeaderIconButton
          onClick={() => {
            resetForm();
            setOpen(true);
          }}
          aria-label="Add label"
          tone="primary"
        >
          <Plus />
        </HeaderIconButton>
      </Header>

      <Main className="lg:max-w-xl">
        {/* Form Dialog */}
        <Dialog
          open={open}
          onOpenChange={(v) => {
            if (!v) resetForm();
          }}
        >
          <DialogContent className="bg-card mx-auto w-11/12">
            <DialogHeader>
              <DialogTitle>{editId ? 'Edit label' : 'Add label'}</DialogTitle>
            </DialogHeader>
            <div className="space-y-3">
              <Input
                type="text"
                placeholder="Label name"
                value={name}
                maxLength={MAX_NAME_LENGTH}
                onChange={(e) => setName(stripLeading(e.target.value))}
              />
              <div>
                <Label className="text-muted-foreground mb-1.5 block text-xs font-medium">
                  Color
                </Label>
                <div className="flex flex-wrap gap-2">
                  {labelColors.map((c) => (
                    <button
                      key={c}
                      onClick={() => setColor(c)}
                      className={`h-7 w-7 rounded-full ${color === c ? 'ring-primary scale-110 ring-2 ring-offset-2' : ''}`}
                      style={{ backgroundColor: c }}
                      aria-label={`Color ${c}`}
                      aria-pressed={color === c}
                    />
                  ))}
                </div>
              </div>
              <div className="flex gap-2">
                <Button onClick={handleSubmit} className="flex-1">
                  {editId ? 'Update' : 'Add'}
                </Button>
                <Button variant="secondary" onClick={resetForm}>
                  Cancel
                </Button>
              </div>
            </div>
          </DialogContent>
        </Dialog>

        {/* List — only when there is something in it, or an empty glass card's hairline
            sits above the empty-state message. */}
        {labels.length > 0 && (
          <div className="card-elevated divide-border divide-y rounded-md px-4">
            {labels.map((label) => (
              <div key={label.id} className="flex items-center justify-between py-3">
                <div className="flex min-w-0 items-center gap-3">
                  <Tag size={16} style={{ color: label.color }} className="shrink-0" aria-hidden />
                  <p className="truncate text-sm font-medium">{label.name}</p>
                </div>
                <div className="flex shrink-0 gap-1">
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={() => handleEdit(label.id)}
                    className="h-8 w-8"
                    aria-label={`Edit ${label.name}`}
                  >
                    <Pencil size={14} className="text-muted-foreground" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    aria-label={`Delete ${label.name}`}
                    onClick={async () => {
                      const confirmed = await confirm({
                        title: `Delete "${label.name}"?`,
                        description:
                          'It will be removed from every transaction tagged with it, and any budget for it is removed.',
                        confirmLabel: 'Delete label',
                      });
                      if (confirmed) deleteLabel(label.id);
                    }}
                    className="h-8 w-8"
                  >
                    <Trash2 size={14} className="text-destructive" />
                  </Button>
                </div>
              </div>
            ))}
          </div>
        )}

        {labels.length === 0 && (
          <div className="py-12 text-center">
            <Tag size={28} className="text-muted-foreground mx-auto mb-3" aria-hidden />
            <p className="text-muted-foreground text-sm">
              No labels yet. Add one to tag your transactions.
            </p>
          </div>
        )}
      </Main>
    </>
  );
}
