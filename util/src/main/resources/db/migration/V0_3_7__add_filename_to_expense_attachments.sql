-- Add filename column to expense_attachments table
ALTER TABLE expense_attachments
ADD COLUMN filename VARCHAR(255) NOT NULL DEFAULT 'unnamed_file';

-- Update existing records to have a meaningful default filename
UPDATE expense_attachments
SET filename = 'attachment_' || id || '.pdf'
WHERE filename = 'unnamed_file';

-- Create index for performance
CREATE INDEX idx_expense_attachments_filename ON expense_attachments(filename); 